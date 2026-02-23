package com.weather.alert.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating alert criteria
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlertCriteriaRequest {
    private String userId;
    private String location;
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private String eventType;
    private String minSeverity;
    private Double maxTemperature;
    private Double minTemperature;
    private Double maxWindSpeed;
    private Double maxPrecipitation;
}
