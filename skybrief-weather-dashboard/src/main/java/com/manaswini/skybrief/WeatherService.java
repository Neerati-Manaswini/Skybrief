package com.manaswini.skybrief;

import com.fasterxml.jackson.databind.JsonNode;
import com.manaswini.skybrief.WeatherReport.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Talks to the free Open-Meteo APIs: one call to turn a city name into coordinates,
 * one call to fetch the forecast. Results are cached for 10 minutes (see application.properties),
 * so repeated searches don't hit the provider again.
 */
@Service
public class WeatherService {

    private final RestClient geocoding = buildClient("https://geocoding-api.open-meteo.com");
    private final RestClient forecast = buildClient("https://api.open-meteo.com");

    private static RestClient buildClient(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Cacheable(cacheNames = "weather", key = "'c:' + #city.toLowerCase() + ':' + #units")
    public WeatherReport byCity(String city, String units) {
        JsonNode response = geocoding.get()
                .uri(u -> u.path("/v1/search")
                        .queryParam("name", "{name}")
                        .queryParam("count", 1)
                        .queryParam("language", "en")
                        .queryParam("format", "json")
                        .build(Map.of("name", city)))
                .retrieve()
                .body(JsonNode.class);

        JsonNode first = response == null ? null : response.path("results").path(0);
        if (first == null || first.isMissingNode()) {
            throw new CityNotFoundException(city);
        }
        Place place = new Place(
                first.path("name").asText(city),
                first.path("admin1").asText(""),
                first.path("country").asText(""),
                first.path("latitude").asDouble(),
                first.path("longitude").asDouble());
        return fetchForecast(place, units);
    }

    @Cacheable(cacheNames = "weather", key = "'g:' + #lat + ',' + #lon + ':' + #units")
    public WeatherReport byCoordinates(double lat, double lon, String units) {
        return fetchForecast(new Place("Your location", "", "", lat, lon), units);
    }

    private WeatherReport fetchForecast(Place place, String units) {
        boolean imperial = "imperial".equals(units);

        JsonNode f = forecast.get()
                .uri(u -> u.path("/v1/forecast")
                        .queryParam("latitude", place.latitude())
                        .queryParam("longitude", place.longitude())
                        .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,is_day")
                        .queryParam("hourly", "temperature_2m,weather_code,precipitation_probability,is_day")
                        .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset")
                        .queryParam("timezone", "auto")
                        .queryParam("forecast_days", 7)
                        .queryParam("temperature_unit", imperial ? "fahrenheit" : "celsius")
                        .queryParam("wind_speed_unit", imperial ? "mph" : "kmh")
                        .build())
                .retrieve()
                .body(JsonNode.class);

        if (f == null || f.path("current").isMissingNode() || f.path("daily").isMissingNode()) {
            throw new UpstreamException("The weather provider returned an unexpected response.");
        }

        JsonNode c = f.path("current");
        WeatherCodes.Condition now = WeatherCodes.of(c.path("weather_code").asInt());
        Current current = new Current(
                c.path("temperature_2m").asDouble(),
                c.path("apparent_temperature").asDouble(),
                c.path("relative_humidity_2m").asInt(),
                c.path("wind_speed_10m").asDouble(),
                c.path("is_day").asInt(1) == 1,
                now.label(), now.icon(), now.theme(),
                c.path("time").asText(""),
                f.path("timezone").asText(""));

        return new WeatherReport(
                place,
                current,
                buildHours(f.path("hourly"), current.time()),
                buildDays(f.path("daily")),
                new Units(imperial ? "°F" : "°C", imperial ? "mph" : "km/h"));
    }

    /** Next 24 hours, starting from the current hour. */
    private List<Hour> buildHours(JsonNode h, String currentTime) {
        JsonNode times = h.path("time");
        String nowKey = hourKey(currentTime);
        int start = 0;
        while (start < times.size() && hourKey(times.get(start).asText()).compareTo(nowKey) < 0) {
            start++;
        }
        List<Hour> hours = new ArrayList<>();
        for (int i = start; i < times.size() && hours.size() < 24; i++) {
            WeatherCodes.Condition cond = WeatherCodes.of(h.path("weather_code").path(i).asInt());
            hours.add(new Hour(
                    times.get(i).asText(),
                    h.path("temperature_2m").path(i).asDouble(),
                    h.path("precipitation_probability").path(i).asInt(0),
                    h.path("is_day").path(i).asInt(1) == 1,
                    cond.icon()));
        }
        return hours;
    }

    private List<Day> buildDays(JsonNode d) {
        JsonNode dates = d.path("time");
        List<Day> days = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            WeatherCodes.Condition cond = WeatherCodes.of(d.path("weather_code").path(i).asInt());
            days.add(new Day(
                    dates.get(i).asText(),
                    d.path("temperature_2m_max").path(i).asDouble(),
                    d.path("temperature_2m_min").path(i).asDouble(),
                    d.path("precipitation_probability_max").path(i).asInt(0),
                    cond.label(), cond.icon(),
                    d.path("sunrise").path(i).asText(""),
                    d.path("sunset").path(i).asText("")));
        }
        return days;
    }

    private static String hourKey(String isoTime) {
        return isoTime.length() >= 13 ? isoTime.substring(0, 13) : isoTime;
    }
}
