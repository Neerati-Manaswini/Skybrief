package com.manaswini.skybrief;

public class CityNotFoundException extends RuntimeException {
    public CityNotFoundException(String city) {
        super("We couldn't find a place called \"" + city + "\". Check the spelling or try a nearby city.");
    }
}
