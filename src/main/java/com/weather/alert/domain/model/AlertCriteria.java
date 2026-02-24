package com.weather.alert.domain.model;

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
        if (!Boolean.TRUE.equals(enabled)) {
            return false;
        }
        
        boolean hasAnyCriteria = false;
        
        // Check location if specified
        if (location != null && !location.isEmpty()) {
            hasAnyCriteria = true;
            if (weatherData.getLocation() == null || 
                !weatherData.getLocation().toLowerCase().contains(location.toLowerCase())) {
                return false;
            }
        }
        
        // Check coordinates and radius if specified
        if (latitude != null && longitude != null && radiusKm != null) {
            hasAnyCriteria = true;
            if (weatherData.getLatitude() != null && weatherData.getLongitude() != null) {
                double distance = calculateDistance(
                    latitude, longitude, 
                    weatherData.getLatitude(), weatherData.getLongitude()
                );
                if (distance > radiusKm) {
                    return false;
                }
            }
        }
        
        // Check event type if specified
        if (eventType != null && !eventType.isEmpty()) {
            hasAnyCriteria = true;
            if (weatherData.getEventType() == null || 
                !weatherData.getEventType().equalsIgnoreCase(eventType)) {
                return false;
            }
        }
        
        // Check severity if specified
        if (minSeverity != null && !minSeverity.isEmpty()) {
            hasAnyCriteria = true;
            if (!meetsSeverityThreshold(weatherData.getSeverity(), minSeverity)) {
                return false;
            }
        }
        
        // Check temperature thresholds
        if (weatherData.getTemperature() != null) {
            if (temperatureThreshold != null && temperatureDirection != null) {
                hasAnyCriteria = true;
                double thresholdInC = thresholdInCelsius(temperatureThreshold, temperatureUnit);
                if (temperatureDirection == TemperatureDirection.ABOVE &&
                        weatherData.getTemperature() > thresholdInC) {
                    return true;
                }
                if (temperatureDirection == TemperatureDirection.BELOW &&
                        weatherData.getTemperature() < thresholdInC) {
                    return true;
                }
            }

            if (maxTemperature != null && weatherData.getTemperature() > maxTemperature) {
                return true;
            }
            if (minTemperature != null && weatherData.getTemperature() < minTemperature) {
                return true;
            }
        }
        
        // Check wind speed
        if (maxWindSpeed != null && weatherData.getWindSpeed() != null) {
            hasAnyCriteria = true;
            if (weatherData.getWindSpeed() > maxWindSpeed) {
                return true;
            }
        }
        
        // Check precipitation
        if (maxPrecipitation != null && weatherData.getPrecipitation() != null) {
            hasAnyCriteria = true;
            if (weatherData.getPrecipitation() > maxPrecipitation) {
                return true;
            }
        }

        if (rainThreshold != null && rainThresholdType != null && weatherData.getPrecipitation() != null) {
            hasAnyCriteria = true;
            if (weatherData.getPrecipitation() >= rainThreshold) {
                return true;
            }
        }
        
        return hasAnyCriteria;
    }

    private double thresholdInCelsius(double threshold, TemperatureUnit unit) {
        TemperatureUnit effectiveUnit = unit != null ? unit : TemperatureUnit.F;
        if (effectiveUnit == TemperatureUnit.C) {
            return threshold;
        }
        return (threshold - 32.0) * 5.0 / 9.0;
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula
        final double R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    private boolean meetsSeverityThreshold(String actualSeverity, String minSeverity) {
        if (actualSeverity == null) return false;
        
        int actual = getSeverityLevel(actualSeverity);
        int minimum = getSeverityLevel(minSeverity);
        return actual >= minimum;
    }
    
    private int getSeverityLevel(String severity) {
        switch (severity.toUpperCase()) {
            case "EXTREME":
                return 4;
            case "SEVERE":
                return 3;
            case "MODERATE":
                return 2;
            case "MINOR":
                return 1;
            default:
                return 0;
        }
    }
}
