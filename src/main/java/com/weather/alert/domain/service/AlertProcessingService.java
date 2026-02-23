package com.weather.alert.domain.service;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Domain service for processing weather alerts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertProcessingService {
    
    private final WeatherDataPort weatherDataPort;
    private final AlertCriteriaRepositoryPort criteriaRepository;
    private final AlertRepositoryPort alertRepository;
    private final NotificationPort notificationPort;
    private final WeatherDataSearchPort searchPort;
    
    /**
     * Process weather data and generate alerts based on user criteria
     */
    public void processWeatherAlerts() {
        log.info("Starting weather alert processing");
        
        // Fetch active weather alerts from NOAA
        List<WeatherData> weatherDataList = weatherDataPort.fetchActiveAlerts();
        log.info("Fetched {} weather data entries", weatherDataList.size());
        
        // Index weather data in Elasticsearch
        weatherDataList.forEach(searchPort::indexWeatherData);
        
        // Get all enabled alert criteria
        List<AlertCriteria> allCriteria = criteriaRepository.findAllEnabled();
        log.info("Found {} enabled alert criteria", allCriteria.size());
        
        List<Alert> generatedAlerts = new ArrayList<>();
        
        // Match weather data against user criteria
        for (WeatherData weatherData : weatherDataList) {
            for (AlertCriteria criteria : allCriteria) {
                if (criteria.matches(weatherData)) {
                    Alert alert = createAlert(criteria, weatherData);
                    Alert savedAlert = alertRepository.save(alert);
                    generatedAlerts.add(savedAlert);
                    
                    // Publish to Kafka for async processing
                    notificationPort.publishAlert(savedAlert);
                    
                    log.info("Generated alert {} for user {} based on criteria {}", 
                            savedAlert.getId(), criteria.getUserId(), criteria.getId());
                }
            }
        }
        
        log.info("Generated {} alerts", generatedAlerts.size());
    }
    
    /**
     * Process alerts for a specific location
     */
    public List<Alert> processAlertsForLocation(double latitude, double longitude) {
        List<WeatherData> weatherDataList = weatherDataPort.fetchAlertsForLocation(latitude, longitude);
        List<AlertCriteria> allCriteria = criteriaRepository.findAllEnabled();
        
        List<Alert> alerts = new ArrayList<>();
        for (WeatherData weatherData : weatherDataList) {
            for (AlertCriteria criteria : allCriteria) {
                if (criteria.matches(weatherData)) {
                    Alert alert = createAlert(criteria, weatherData);
                    alerts.add(alertRepository.save(alert));
                }
            }
        }
        return alerts;
    }
    
    private Alert createAlert(AlertCriteria criteria, WeatherData weatherData) {
        return Alert.builder()
                .id(UUID.randomUUID().toString())
                .userId(criteria.getUserId())
                .criteriaId(criteria.getId())
                .weatherDataId(weatherData.getId())
                .eventType(weatherData.getEventType())
                .severity(weatherData.getSeverity())
                .headline(weatherData.getHeadline())
                .description(weatherData.getDescription())
                .location(weatherData.getLocation())
                .alertTime(Instant.now())
                .status(Alert.AlertStatus.PENDING)
                .build();
    }
}
