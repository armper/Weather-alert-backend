package com.weather.alert.domain.port;

import com.weather.alert.domain.model.PagedResult;
import com.weather.alert.domain.model.WeatherData;

import java.time.Instant;
import java.util.List;

/**
 * Port for weather data search (Elasticsearch)
 */
public interface WeatherDataSearchPort {
    
    void indexWeatherData(WeatherData weatherData);

    PagedResult<WeatherData> getActiveWeatherData(int page, int size);
    
    List<WeatherData> searchByLocation(String location);
    
    List<WeatherData> searchByEventType(String eventType);
    
    List<WeatherData> searchBySeverity(String severity);

    long deleteWeatherDataOlderThan(Instant cutoff);
}
