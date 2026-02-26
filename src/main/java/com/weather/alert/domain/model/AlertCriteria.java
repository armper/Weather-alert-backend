package com.weather.alert.domain.model;

import com.weather.alert.domain.service.evaluation.AlertCriteriaRuleEvaluator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing user alert criteria
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stored user alert criteria")
public class AlertCriteria {
    public enum TemperatureDirection {
        BELOW,
        ABOVE
    }

    public enum RainThresholdType {
        PROBABILITY,
        AMOUNT
    }

    public enum TemperatureUnit {
        F,
        C
    }

    @Schema(example = "ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6")
    private String id;

    @Schema(description = "User-defined alert name", example = "Annoying Winds")
    private String name;

    @Schema(example = "user-123")
    private String userId;

    @Schema(example = "Seattle")
    private String location;

    @Schema(example = "47.6062")
    private Double latitude;

    @Schema(example = "-122.3321")
    private Double longitude;

    @Schema(example = "50")
    private Double radiusKm;

    @Schema(example = "Tornado Warning")
    private String eventType;

    @Schema(allowableValues = {"MINOR", "MODERATE", "SEVERE", "EXTREME"}, example = "SEVERE")
    private String minSeverity;

    @Schema(example = "35")
    private Double maxTemperature;

    @Schema(example = "-5")
    private Double minTemperature;

    @Schema(example = "80")
    private Double maxWindSpeed;

    @Schema(example = "15")
    private Double maxPrecipitation;

    @Schema(description = "Single temperature threshold value", example = "60")
    private Double temperatureThreshold;

    @Schema(allowableValues = {"BELOW", "ABOVE"}, example = "BELOW")
    private TemperatureDirection temperatureDirection;

    @Schema(description = "Rain threshold for triggering an alert", example = "40")
    private Double rainThreshold;

    @Schema(allowableValues = {"PROBABILITY", "AMOUNT"}, example = "PROBABILITY")
    private RainThresholdType rainThresholdType;

    @Schema(description = "Evaluate current weather conditions", example = "true")
    private Boolean monitorCurrent;

    @Schema(description = "Evaluate forecast conditions", example = "true")
    private Boolean monitorForecast;

    @Schema(description = "Forecast lookahead window in hours", example = "48")
    private Integer forecastWindowHours;

    @Schema(allowableValues = {"F", "C"}, example = "F")
    private TemperatureUnit temperatureUnit;

    @Schema(description = "Notify once for each detected condition/event", example = "true")
    private Boolean oncePerEvent;

    @Schema(description = "Cooldown/rearm window in minutes before a similar alert can fire again", example = "120")
    private Integer rearmWindowMinutes;

    @Schema(example = "true")
    private Boolean enabled;
    
    public boolean matches(WeatherData weatherData) {
        return AlertCriteriaRuleEvaluator.defaultInstance().matches(this, weatherData);
    }
}
