package com.weather.alert.domain.model;

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
public class AlertCriteria {
    private String id;
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
    private Boolean enabled;
    
    public boolean matches(WeatherData weatherData) {
        if (!Boolean.TRUE.equals(enabled) || weatherData == null) {
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
        if (maxTemperature != null || minTemperature != null) {
            hasAnyCriteria = true;
            if (weatherData.getTemperature() == null) {
                return false;
            }
            boolean temperatureThresholdMet = (maxTemperature != null && weatherData.getTemperature() > maxTemperature)
                    || (minTemperature != null && weatherData.getTemperature() < minTemperature);
            if (!temperatureThresholdMet) {
                return false;
            }
        }
        
        // Check wind speed
        if (maxWindSpeed != null) {
            hasAnyCriteria = true;
            if (weatherData.getWindSpeed() == null || weatherData.getWindSpeed() <= maxWindSpeed) {
                return false;
            }
        }
        
        // Check precipitation
        if (maxPrecipitation != null) {
            hasAnyCriteria = true;
            if (weatherData.getPrecipitation() == null || weatherData.getPrecipitation() <= maxPrecipitation) {
                return false;
            }
        }
        
        return hasAnyCriteria;
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
