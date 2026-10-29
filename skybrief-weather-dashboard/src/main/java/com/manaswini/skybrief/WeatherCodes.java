package com.manaswini.skybrief;

/**
 * Translates WMO weather codes (used by Open-Meteo) into a readable label,
 * an icon key and a colour theme for the frontend.
 */
public final class WeatherCodes {

    public record Condition(String label, String icon, String theme) {}

    private WeatherCodes() {}

    public static Condition of(int code) {
        return switch (code) {
            case 0 -> new Condition("Clear sky", "clear", "clear");
            case 1 -> new Condition("Mainly clear", "clear", "clear");
            case 2 -> new Condition("Partly cloudy", "partly", "cloudy");
            case 3 -> new Condition("Overcast", "cloudy", "cloudy");
            case 45, 48 -> new Condition("Fog", "fog", "fog");
            case 51, 53, 55 -> new Condition("Drizzle", "drizzle", "rain");
            case 56, 57 -> new Condition("Freezing drizzle", "drizzle", "rain");
            case 61 -> new Condition("Light rain", "rain", "rain");
            case 63 -> new Condition("Rain", "rain", "rain");
            case 65 -> new Condition("Heavy rain", "rain", "rain");
            case 66, 67 -> new Condition("Freezing rain", "rain", "rain");
            case 71, 73, 75 -> new Condition("Snowfall", "snow", "snow");
            case 77 -> new Condition("Snow grains", "snow", "snow");
            case 80, 81 -> new Condition("Rain showers", "drizzle", "rain");
            case 82 -> new Condition("Violent rain showers", "rain", "rain");
            case 85, 86 -> new Condition("Snow showers", "snow", "snow");
            case 95 -> new Condition("Thunderstorm", "storm", "storm");
            case 96, 99 -> new Condition("Thunderstorm with hail", "storm", "storm");
            default -> new Condition("Unknown", "cloudy", "cloudy");
        };
    }
}
