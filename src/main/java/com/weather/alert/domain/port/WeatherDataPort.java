package com.weather.alert.domain.port;

import com.weather.alert.domain.model.WeatherData;

import java.util.List;
import java.util.Optional;

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

    /**
     * Fetch latest current conditions for a coordinate.
     */
    Optional<WeatherData> fetchCurrentConditions(double latitude, double longitude);

    /**
     * Fetch hourly forecast conditions for a coordinate, bounded by a forecast window.
     */
    List<WeatherData> fetchForecastConditions(double latitude, double longitude, int forecastWindowHours);
}
