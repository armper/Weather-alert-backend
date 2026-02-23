package com.weather.alert.domain.port;

import com.weather.alert.domain.model.WeatherData;

import java.util.List;

/**
 * Port for fetching weather data from external sources (NOAA)
 */
public interface WeatherDataPort {
    
    /**
     * Fetch active weather alerts
     */
    List<WeatherData> fetchActiveAlerts();
    
    /**
     * Fetch weather alerts for a specific location
     */
    List<WeatherData> fetchAlertsForLocation(double latitude, double longitude);
    
    /**
     * Fetch weather alerts for a specific state
     */
    List<WeatherData> fetchAlertsForState(String stateCode);
}
