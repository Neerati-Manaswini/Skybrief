package com.manaswini.skybrief;

import java.util.List;

/** The clean JSON shape the frontend receives from /api/weather. */
public record WeatherReport(Place place, Current current, List<Hour> hourly, List<Day> daily, Units units) {

    public record Place(String name, String region, String country, double latitude, double longitude) {}

    public record Current(double temperature, double feelsLike, int humidity, double windSpeed,
                          boolean day, String label, String icon, String theme, String time, String timezone) {}

    public record Hour(String time, double temperature, int precipChance, boolean day, String icon) {}

    public record Day(String date, double max, double min, int precipChance,
                      String label, String icon, String sunrise, String sunset) {}

    public record Units(String temperature, String wind) {}
}
