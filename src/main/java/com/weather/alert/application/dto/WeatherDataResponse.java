package com.weather.alert.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Weather alert data fetched from NOAA")
public class WeatherDataResponse {
    @Schema(example = "NWS-IDP-PROD-1234567")
    private String id;

    @Schema(example = "Seattle, WA")
    private String location;

    @Schema(example = "Flood Warning")
    private String eventType;

    @Schema(example = "MODERATE")
    private String severity;

    @Schema(example = "Flood Warning issued February 24 at 9:42AM PST")
    private String headline;

    @Schema(example = "Minor flooding is occurring along the Snoqualmie River near Carnation.")
    private String description;

    @Schema(example = "2026-02-24T17:42:00Z")
    private String onset;

    @Schema(example = "2026-02-25T01:00:00Z")
    private String expires;

    @Schema(description = "Temperature in Celsius", example = "14.0")
    private Double temperature;

    @Schema(description = "Wind speed in km/h", example = "8.0")
    private Double windSpeed;

    @Schema(description = "Rain probability percentage for forecast periods", example = "40.0")
    private Double precipitationProbability;

    @Schema(description = "Measured precipitation amount in mm for current observations", example = "2.0")
    private Double precipitationAmount;

    @Schema(description = "Relative humidity percentage", example = "24.0")
    private Double humidity;

    @Schema(description = "Dew point in Celsius", example = "18.5")
    private Double dewPoint;

    @Schema(description = "Wind gust in km/h", example = "42.0")
    private Double windGust;

    @Schema(description = "Sky cover percentage", example = "90.0")
    private Double skyCover;

    @Schema(description = "NWPS gauge identifier", example = "ABNG1")
    private String riverGaugeId;

    @Schema(description = "Latest observed river stage", example = "17.2")
    private Double riverObservedStage;

    @Schema(description = "Latest forecast river stage", example = "18.6")
    private Double riverForecastStage;

    @Schema(description = "Official flood stage threshold", example = "17.0")
    private Double riverFloodStage;

    @Schema(description = "Official action stage threshold", example = "14.0")
    private Double riverActionStage;

    @Schema(description = "NWPS observed flood category", example = "minor")
    private String riverObservedCategory;

    @Schema(description = "NWPS forecast flood category", example = "moderate")
    private String riverForecastCategory;

    @Schema(description = "NWPS stage unit", example = "ft")
    private String riverStageUnit;

    @Schema(description = "Distance from requested point to resolved gauge in kilometers", example = "12.4")
    private Double riverDistanceKm;

    @Schema(example = "2026-02-24T19:10:00Z")
    private String timestamp;
}
