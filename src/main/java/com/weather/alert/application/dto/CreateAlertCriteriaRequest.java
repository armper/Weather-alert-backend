package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Create or update alert criteria for a user")
public class CreateAlertCriteriaRequest {
    @NotBlank(message = "userId is required")
    @Schema(description = "User identifier that owns this criteria", example = "user-123")
    private String userId;

    @Schema(description = "Location name filter (case-insensitive contains match)", example = "Seattle")
    private String location;

    @DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "latitude must be <= 90")
    @Schema(description = "Center latitude for radius-based matching", example = "47.6062")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "longitude must be <= 180")
    @Schema(description = "Center longitude for radius-based matching", example = "-122.3321")
    private Double longitude;

    @DecimalMin(value = "0.0", message = "radiusKm must be >= 0")
    @Schema(description = "Matching radius in kilometers", example = "50")
    private Double radiusKm;

    @Schema(description = "Weather event type to match exactly", example = "Tornado Warning")
    private String eventType;

    @Schema(description = "Minimum severity threshold", allowableValues = {"MINOR", "MODERATE", "SEVERE", "EXTREME"}, example = "SEVERE")
    private String minSeverity;

    @Schema(description = "Trigger when observed temperature is above this threshold (C)", example = "35")
    private Double maxTemperature;

    @Schema(description = "Trigger when observed temperature is below this threshold (C)", example = "-5")
    private Double minTemperature;

    @Schema(description = "Trigger when wind speed is above this threshold (km/h)", example = "80")
    private Double maxWindSpeed;

    @Schema(description = "Trigger when precipitation is above this threshold (mm/h)", example = "15")
    private Double maxPrecipitation;
}
