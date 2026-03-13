package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Domain model representing weather data from NOAA
 */
@Data
@Builder(toBuilder = true)
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
    private List<String> affectedZoneIds;
    private List<String> ugcCodes;
    private List<String> sameCodes;
    private Double temperature;
    private Double windSpeed;
    private Double precipitation;
    private Double precipitationProbability;
    private Double precipitationAmount;
    private Double humidity;
    private Double dewPoint;
    private Double windGust;
    private Double skyCover;
    private String riverGaugeId;
    private Double riverObservedStage;
    private Double riverForecastStage;
    private Double riverFloodStage;
    private Double riverActionStage;
    private String riverObservedCategory;
    private String riverForecastCategory;
    private String riverStageUnit;
    private Double riverDistanceKm;
    private Instant timestamp;

    private Double apparentTemperature;
    private Double windChill;
    private Double heatIndex;
    private Double visibility;
    private Double windDirection;
    private Double snowfallAmount;
    private Double iceAccumulation;
    private Double probabilityOfThunder;
    private Double ceilingHeight;
}
