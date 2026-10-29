# SkyBrief

A weather dashboard with a Spring Boot backend and a responsive, theme-aware frontend. Search any city (or use your location) for current conditions, the next 24 hours, and a 7-day forecast. The page background changes with the weather and the time of day.

No API key needed: it uses the free [Open-Meteo](https://open-meteo.com/) APIs.

## Features
- City search with geocoding, plus "use my location"
- Current conditions: temperature, feels like, humidity, wind, chance of rain, sunrise and sunset
- Hourly strip for the next 24 hours and a 7-day forecast with temperature range bars
- Celsius / Fahrenheit toggle, remembered between visits
- Recent searches saved in the browser
- Friendly error messages (city not found, provider unavailable)

## How it works
```
Browser (HTML/CSS/JS)  ->  GET /api/weather?city=Hyderabad&units=metric
                                |
                         WeatherController   validates input
                                |
                         WeatherService      geocodes the city, fetches the forecast,
                                |            maps it to a clean JSON shape
                         Caffeine cache      10-minute cache per city and unit
                                |
                         Open-Meteo APIs
```
The backend acts as a proxy, so the browser never calls the weather provider directly and responses are cached.

### API
| Request | Description |
|---|---|
| `GET /api/weather?city=Mumbai` | Forecast by city name |
| `GET /api/weather?lat=17.38&lon=78.48` | Forecast by coordinates |
| `&units=imperial` | Optional. `metric` (default) or `imperial` |

Errors return JSON like `{"error": "..."}` with status 400 (bad input), 404 (city not found) or 502 (provider unavailable).

## Run it
Requirements: JDK 17 or newer and an internet connection. Use Maven, or open the folder in Eclipse / IntelliJ / VS Code.

```bash
mvn spring-boot:run
```
Open http://localhost:8080

In Eclipse: **File > Import > Maven > Existing Maven Projects**, choose this folder, then run `SkyBriefApplication.java` as a Java Application.

## Tech stack
Java 17, Spring Boot 3, Spring Web (RestClient), Spring Cache + Caffeine, JUnit 5, HTML, CSS, vanilla JavaScript.

## Project layout
```
src/main/java/com/manaswini/skybrief
  WeatherController      REST endpoint and input validation
  WeatherService         calls Open-Meteo, maps and caches results
  WeatherCodes           weather code -> label, icon, colour theme
  WeatherReport          response model (Java records)
  ApiExceptionHandler    JSON error responses
src/main/resources/static
  index.html, style.css, app.js
```

## Ideas to extend
- Add a 5-day air quality or UV index panel
- Save favourite cities per user with Spring Security and a database
- Add `MockRestServiceServer` tests for `WeatherService`
- Add a Dockerfile and deploy to Render
