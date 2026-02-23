package com.weather.alert.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for weather data response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDataResponse {
    private String id;
    private String location;
    private String eventType;
    private String severity;
    private String headline;
    private String description;
    private String onset;
    private String expires;
}
