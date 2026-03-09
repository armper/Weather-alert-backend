package com.weather.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WeatherAlertApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(WeatherAlertApplication.class, args);
    }
}
