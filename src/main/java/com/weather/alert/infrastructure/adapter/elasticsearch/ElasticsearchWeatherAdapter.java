package com.weather.alert.infrastructure.adapter.elasticsearch;

import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.WeatherDataSearchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchWeatherAdapter implements WeatherDataSearchPort {
    
    private final ElasticsearchWeatherRepository repository;
    
    @Override
    public void indexWeatherData(WeatherData weatherData) {
        try {
            WeatherDataDocument document = toDocument(weatherData);
            repository.save(document);
            log.debug("Indexed weather data: {}", weatherData.getId());
        } catch (Exception e) {
            log.error("Error indexing weather data", e);
        }
    }
    
    @Override
    public List<WeatherData> searchByLocation(String location) {
        return repository.findByLocation(location).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WeatherData> searchByEventType(String eventType) {
        return repository.findByEventType(eventType).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WeatherData> searchBySeverity(String severity) {
        return repository.findBySeverity(severity).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    private WeatherDataDocument toDocument(WeatherData domain) {
        return WeatherDataDocument.builder()
                .id(domain.getId())
                .location(domain.getLocation())
                .latitude(domain.getLatitude())
                .longitude(domain.getLongitude())
                .eventType(domain.getEventType())
                .severity(domain.getSeverity())
                .headline(domain.getHeadline())
                .description(domain.getDescription())
                .onset(domain.getOnset())
                .expires(domain.getExpires())
                .status(domain.getStatus())
                .timestamp(domain.getTimestamp())
                .build();
    }
    
    private WeatherData toDomain(WeatherDataDocument document) {
        return WeatherData.builder()
                .id(document.getId())
                .location(document.getLocation())
                .latitude(document.getLatitude())
                .longitude(document.getLongitude())
                .eventType(document.getEventType())
                .severity(document.getSeverity())
                .headline(document.getHeadline())
                .description(document.getDescription())
                .onset(document.getOnset())
                .expires(document.getExpires())
                .status(document.getStatus())
                .timestamp(document.getTimestamp())
                .build();
    }
}
