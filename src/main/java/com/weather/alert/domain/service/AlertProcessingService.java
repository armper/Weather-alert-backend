package com.weather.alert.domain.service;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.*;
import com.weather.alert.domain.service.evaluation.AlertCriteriaRuleEvaluator;
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
    private final AlertCriteriaRuleEvaluator criteriaRuleEvaluator;
    
    /**
     * Process weather data and generate alerts based on user criteria
     */
    public void processWeatherAlerts() {
        log.info("Starting weather alert processing");
        
        List<WeatherData> activeWeatherAlerts = weatherDataPort.fetchActiveAlerts();
        log.info("Fetched {} active NOAA weather alerts", activeWeatherAlerts.size());
        
        activeWeatherAlerts.forEach(searchPort::indexWeatherData);
        
        List<AlertCriteria> allCriteria = criteriaRepository.findAllEnabled();
        log.info("Found {} enabled alert criteria", allCriteria.size());
        
        List<Alert> generatedAlerts = new ArrayList<>();
        
        for (AlertCriteria criteria : allCriteria) {
            generatedAlerts.addAll(generateAlertsForCriteria(criteria, activeWeatherAlerts, true));
            generatedAlerts.addAll(generateConditionAlertsForCriteria(criteria, true));
        }
        
        log.info("Generated {} alerts", generatedAlerts.size());
    }

    /**
     * Immediately evaluate a newly-created criteria so users can be notified right away
     * when conditions are already true.
     */
    public List<Alert> processCriteriaImmediately(AlertCriteria criteria) {
        if (criteria == null || !Boolean.TRUE.equals(criteria.getEnabled())) {
            return List.of();
        }

        log.info("Running immediate evaluation for criteria {} (user={})", criteria.getId(), criteria.getUserId());
        List<Alert> generatedAlerts = new ArrayList<>();

        List<WeatherData> activeWeatherAlerts = weatherDataPort.fetchActiveAlerts();
        activeWeatherAlerts.forEach(searchPort::indexWeatherData);
        generatedAlerts.addAll(generateAlertsForCriteria(criteria, activeWeatherAlerts, true));
        generatedAlerts.addAll(generateConditionAlertsForCriteria(criteria, true));

        log.info("Immediate evaluation generated {} alerts for criteria {}", generatedAlerts.size(), criteria.getId());
        return generatedAlerts;
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
                if (criteriaRuleEvaluator.matches(criteria, weatherData)) {
                    alerts.add(saveAndPublishAlert(criteria, weatherData, false));
                }
            }
        }
        return alerts;
    }

    private List<Alert> generateAlertsForCriteria(
            AlertCriteria criteria,
            List<WeatherData> weatherDataList,
            boolean publish) {
        if (criteria == null || !Boolean.TRUE.equals(criteria.getEnabled())) {
            return List.of();
        }

        List<Alert> generatedAlerts = new ArrayList<>();
        for (WeatherData weatherData : weatherDataList) {
            if (!criteriaRuleEvaluator.matches(criteria, weatherData)) {
                continue;
            }
            generatedAlerts.add(saveAndPublishAlert(criteria, weatherData, publish));
        }
        return generatedAlerts;
    }

    private List<Alert> generateConditionAlertsForCriteria(AlertCriteria criteria, boolean publish) {
        if (criteria == null || !Boolean.TRUE.equals(criteria.getEnabled())) {
            return List.of();
        }
        if (!criteriaRuleEvaluator.hasWeatherConditionRules(criteria)) {
            return List.of();
        }
        if (criteria.getLatitude() == null || criteria.getLongitude() == null) {
            log.debug("Skipping condition evaluation for criteria {}: missing latitude/longitude", criteria.getId());
            return List.of();
        }

        List<Alert> generatedAlerts = new ArrayList<>();

        if (shouldMonitorCurrent(criteria)) {
            weatherDataPort.fetchCurrentConditions(criteria.getLatitude(), criteria.getLongitude())
                    .ifPresent(current -> {
                        searchPort.indexWeatherData(current);
                        if (criteriaRuleEvaluator.matches(criteria, current)) {
                            generatedAlerts.add(saveAndPublishAlert(criteria, current, publish));
                        }
                    });
        }

        if (shouldMonitorForecast(criteria)) {
            int forecastWindowHours = normalizeForecastWindowHours(criteria.getForecastWindowHours());
            List<WeatherData> forecast = weatherDataPort.fetchForecastConditions(
                    criteria.getLatitude(),
                    criteria.getLongitude(),
                    forecastWindowHours
            );
            forecast.forEach(searchPort::indexWeatherData);

            // In this chunk, send one alert for the first matching forecast period.
            forecast.stream()
                    .filter(weatherData -> criteriaRuleEvaluator.matches(criteria, weatherData))
                    .findFirst()
                    .ifPresent(match -> generatedAlerts.add(saveAndPublishAlert(criteria, match, publish)));
        }

        return generatedAlerts;
    }

    private Alert saveAndPublishAlert(AlertCriteria criteria, WeatherData weatherData, boolean publish) {
        Alert alert = createAlert(criteria, weatherData);
        Alert savedAlert = alertRepository.save(alert);
        if (publish) {
            notificationPort.publishAlert(savedAlert);
        }
        log.info("Generated alert {} for user {} based on criteria {}",
                savedAlert.getId(), criteria.getUserId(), criteria.getId());
        return savedAlert;
    }

    private boolean shouldMonitorCurrent(AlertCriteria criteria) {
        return criteria.getMonitorCurrent() == null || criteria.getMonitorCurrent();
    }

    private boolean shouldMonitorForecast(AlertCriteria criteria) {
        return criteria.getMonitorForecast() == null || criteria.getMonitorForecast();
    }

    private int normalizeForecastWindowHours(Integer hours) {
        int value = hours == null ? 48 : hours;
        return Math.max(1, Math.min(value, 168));
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
