package com.weather.alert.domain.port;

import com.weather.alert.domain.model.NwsProduct;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.model.HydrologyQuery;

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

    /**
     * Fetch active alerts and include provider availability status.
     */
    default WeatherFetchResult<List<WeatherData>> fetchActiveAlertsWithStatus() {
        return WeatherFetchResult.success(fetchActiveAlerts());
    }

    /**
     * Fetch latest current conditions and include provider availability status.
     */
    default WeatherFetchResult<Optional<WeatherData>> fetchCurrentConditionsWithStatus(double latitude, double longitude) {
        return WeatherFetchResult.success(fetchCurrentConditions(latitude, longitude));
    }

    /**
     * Fetch forecast conditions and include provider availability status.
     */
    default WeatherFetchResult<List<WeatherData>> fetchForecastConditionsWithStatus(
            double latitude,
            double longitude,
            int forecastWindowHours) {
        return WeatherFetchResult.success(fetchForecastConditions(latitude, longitude, forecastWindowHours));
    }

    /**
     * Fetch latest NWPS observed river conditions for a gauge or nearby coordinate.
     */
    default Optional<WeatherData> fetchHydrologyCurrentConditions(HydrologyQuery query) {
        return Optional.empty();
    }

    /**
     * Fetch latest NWPS forecast river conditions for a gauge or nearby coordinate.
     */
    default Optional<WeatherData> fetchHydrologyForecastConditions(HydrologyQuery query) {
        return Optional.empty();
    }

    /**
     * Fetch latest NWPS observed river conditions and include provider availability status.
     */
    default WeatherFetchResult<Optional<WeatherData>> fetchHydrologyCurrentConditionsWithStatus(HydrologyQuery query) {
        return WeatherFetchResult.success(fetchHydrologyCurrentConditions(query));
    }

    /**
     * Fetch latest NWPS forecast river conditions and include provider availability status.
     */
    default WeatherFetchResult<Optional<WeatherData>> fetchHydrologyForecastConditionsWithStatus(HydrologyQuery query) {
        return WeatherFetchResult.success(fetchHydrologyForecastConditions(query));
    }

    /**
     * Fetch historical observations for a coordinate over the given number of hours.
     */
    default List<WeatherData> fetchObservationHistory(double latitude, double longitude, int hours) {
        return List.of();
    }

    /**
     * Fetch the 7-day period forecast for a coordinate.
     */
    default List<WeatherData> fetchDailyForecast(double latitude, double longitude) {
        return List.of();
    }

    /**
     * Fetch a single NOAA alert by its identifier.
     */
    default Optional<WeatherData> fetchAlertById(String alertId) {
        return Optional.empty();
    }

    /**
     * Fetch NWS text products optionally filtered by type code and/or location code.
     */
    default List<NwsProduct> fetchProductsByType(String typeCode, String locationCode) {
        return List.of();
    }

    /**
     * Fetch a single NWS text product by its identifier.
     */
    default Optional<NwsProduct> fetchProductById(String productId) {
        return Optional.empty();
    }

    /**
     * Fetch a zone-based forecast from NWS.
     */
    default List<WeatherData> fetchZoneForecast(String zoneType, String zoneId) {
        return List.of();
    }
}

