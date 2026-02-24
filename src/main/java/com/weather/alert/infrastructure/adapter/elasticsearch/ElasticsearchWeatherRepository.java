package com.weather.alert.infrastructure.adapter.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElasticsearchWeatherRepository extends ElasticsearchRepository<WeatherDataDocument, String> {
    List<WeatherDataDocument> findByLocationContainingIgnoreCase(String location);
    List<WeatherDataDocument> findByEventTypeContainingIgnoreCase(String eventType);
    List<WeatherDataDocument> findBySeverity(String severity);
}
