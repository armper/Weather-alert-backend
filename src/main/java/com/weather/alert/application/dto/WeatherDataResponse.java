package com.weather.alert.application.dto;

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

    @Schema(example = "2026-02-24T19:10:00Z")
    private String timestamp;
}
