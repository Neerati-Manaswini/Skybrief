package com.manaswini.skybrief;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SkyBriefApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyBriefApplication.class, args);
    }
}
