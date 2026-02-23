package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain model representing weather data from NOAA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherData {
    private String id;
    private String location;
    private Double latitude;
    private Double longitude;
    private String eventType;
    private String severity;
    private String headline;
    private String description;
    private Instant onset;
    private Instant expires;
    private String status;
    private String messageType;
    private String category;
    private String urgency;
    private String certainty;
    private Double temperature;
    private Double windSpeed;
    private Double precipitation;
    private Double humidity;
    private Instant timestamp;
}
