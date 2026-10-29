package com.manaswini.skybrief;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    private final WeatherService service;

    public WeatherController(WeatherService service) {
        this.service = service;
    }

    /**
     * GET /api/weather?city=Hyderabad&units=metric
     * GET /api/weather?lat=17.38&lon=78.48&units=imperial
     */
    @GetMapping("/api/weather")
    public WeatherReport weather(@RequestParam(name = "city", required = false) String city,
                                 @RequestParam(name = "lat", required = false) Double lat,
                                 @RequestParam(name = "lon", required = false) Double lon,
                                 @RequestParam(name = "units", defaultValue = "metric") String units) {
        if (!units.equals("metric") && !units.equals("imperial")) {
            throw new IllegalArgumentException("units must be 'metric' or 'imperial'.");
        }
        if (city != null && !city.isBlank()) {
            String trimmed = city.trim();
            if (trimmed.length() > 80) {
                throw new IllegalArgumentException("City name is too long.");
            }
            return service.byCity(trimmed, units);
        }
        if (lat != null && lon != null) {
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                throw new IllegalArgumentException("Coordinates are out of range.");
            }
            // Round to ~1 km so nearby requests share one cache entry
            return service.byCoordinates(round2(lat), round2(lon), units);
        }
        throw new IllegalArgumentException("Provide either a city, or both lat and lon.");
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
