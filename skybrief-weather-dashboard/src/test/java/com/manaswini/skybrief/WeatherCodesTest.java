package com.manaswini.skybrief;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherCodesTest {

    @Test
    void clearSkyMapsToClearTheme() {
        assertEquals("Clear sky", WeatherCodes.of(0).label());
        assertEquals("clear", WeatherCodes.of(0).theme());
    }

    @Test
    void rainCodesMapToRainTheme() {
        assertEquals("rain", WeatherCodes.of(61).theme());
        assertEquals("rain", WeatherCodes.of(81).theme());
    }

    @Test
    void thunderstormCodesMapToStormTheme() {
        assertEquals("storm", WeatherCodes.of(95).theme());
        assertEquals("storm", WeatherCodes.of(99).theme());
    }

    @Test
    void unknownCodeFallsBackSafely() {
        assertEquals("Unknown", WeatherCodes.of(12345).label());
    }
}
