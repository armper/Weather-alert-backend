package com.weather.alert.domain.port;

import com.weather.alert.domain.model.PagedResult;
import com.weather.alert.domain.model.WeatherData;

import java.time.Instant;
import java.util.List;

/**
 * Port for the indexed weather-data read model.
 */
public interface WeatherDataSearchPort {
    
    void indexWeatherData(WeatherData weatherData);

    PagedResult<WeatherData> getActiveWeatherData(int page, int size);

    List<WeatherData> searchByLocation(String location, int limit);

    List<WeatherData> searchByEventType(String eventType, int limit);

    List<WeatherData> searchBySeverity(String severity, int limit);

    long deleteWeatherDataOlderThan(Instant cutoff);
}
