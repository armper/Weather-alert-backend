package com.weather.alert.application.dto;

import com.weather.alert.domain.model.AlertCriteria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@Schema(
        description = "Create or update alert criteria for a user",
        example = """
                {
                  "userId": "dev-admin",
                  "location": "Orlando",
                  "eventType": "Rain",
                  "minSeverity": "MODERATE",
                  "temperatureThreshold": 60,
                  "temperatureDirection": "BELOW",
                  "temperatureUnit": "F",
                  "rainThreshold": 40,
                  "rainThresholdType": "PROBABILITY",
                  "monitorCurrent": true,
                  "monitorForecast": true,
                  "forecastWindowHours": 48,
                  "oncePerEvent": true,
                  "rearmWindowMinutes": 120
                }
                """)
public class CreateAlertCriteriaRequest {
    @Schema(description = "User identifier that owns this criteria (optional for non-admin; inferred from JWT subject)", example = "user-123")
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

    @Schema(description = "Single temperature threshold value", example = "60")
    private Double temperatureThreshold;

    @Schema(description = "Direction for temperature threshold", example = "BELOW")
    private AlertCriteria.TemperatureDirection temperatureDirection;

    @DecimalMin(value = "0.0", message = "rainThreshold must be >= 0")
    @Schema(description = "Rain threshold used for alerting", example = "40")
    private Double rainThreshold;

    @Schema(description = "Rain threshold mode", example = "PROBABILITY")
    private AlertCriteria.RainThresholdType rainThresholdType;

    @Schema(description = "Evaluate current weather conditions", example = "true")
    private Boolean monitorCurrent;

    @Schema(description = "Evaluate forecast conditions", example = "true")
    private Boolean monitorForecast;

    @Min(value = 1, message = "forecastWindowHours must be >= 1")
    @Max(value = 168, message = "forecastWindowHours must be <= 168")
    @Schema(description = "Forecast lookahead window in hours", example = "48")
    private Integer forecastWindowHours;

    @Schema(description = "Preferred temperature unit for thresholds", example = "F")
    private AlertCriteria.TemperatureUnit temperatureUnit;

    @Schema(description = "Notify only once per detected event/condition", example = "true")
    private Boolean oncePerEvent;

    @Min(value = 0, message = "rearmWindowMinutes must be >= 0")
    @Max(value = 10080, message = "rearmWindowMinutes must be <= 10080")
    @Schema(description = "Rearm/cooldown window in minutes", example = "120")
    private Integer rearmWindowMinutes;

    @AssertTrue(message = "temperatureThreshold and temperatureDirection must be provided together")
    public boolean isTemperatureThresholdPairValid() {
        return (temperatureThreshold == null && temperatureDirection == null)
                || (temperatureThreshold != null && temperatureDirection != null);
    }

    @AssertTrue(message = "rainThreshold and rainThresholdType must be provided together")
    public boolean isRainThresholdPairValid() {
        return (rainThreshold == null && rainThresholdType == null)
                || (rainThreshold != null && rainThresholdType != null);
    }

    @AssertTrue(message = "At least one monitoring mode must be enabled")
    public boolean isMonitoringModeValid() {
        if (monitorCurrent == null && monitorForecast == null) {
            return true;
        }
        return Boolean.TRUE.equals(monitorCurrent) || Boolean.TRUE.equals(monitorForecast);
    }

    @AssertTrue(message = "latitude and longitude must be provided together")
    public boolean isCoordinatePairValid() {
        return (latitude == null && longitude == null)
                || (latitude != null && longitude != null);
    }

    @AssertTrue(message = "radiusKm requires both latitude and longitude")
    public boolean isRadiusCoordinatesValid() {
        return radiusKm == null || (latitude != null && longitude != null);
    }

    @AssertTrue(message = "latitude and longitude are required when using temperatureThreshold or rainThreshold")
    public boolean isCoordinatesPresentForConditionThresholds() {
        boolean conditionThresholdConfigured = temperatureThreshold != null || rainThreshold != null;
        return !conditionThresholdConfigured || (latitude != null && longitude != null);
    }

    @AssertTrue(message = "forecastWindowHours can only be set when monitorForecast is enabled")
    public boolean isForecastWindowUsageValid() {
        return forecastWindowHours == null || !Boolean.FALSE.equals(monitorForecast);
    }

    @AssertTrue(message = "rainThreshold must be <= 100 when rainThresholdType is PROBABILITY")
    public boolean isProbabilityThresholdValid() {
        if (rainThreshold == null || rainThresholdType == null) {
            return true;
        }
        return rainThresholdType != AlertCriteria.RainThresholdType.PROBABILITY || rainThreshold <= 100.0;
    }
}
