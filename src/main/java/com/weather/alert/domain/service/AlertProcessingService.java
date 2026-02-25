package com.weather.alert.domain.service;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.AlertCriteriaState;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.*;
import com.weather.alert.domain.service.evaluation.AlertCriteriaRuleEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final AlertCriteriaStateRepositoryPort criteriaStateRepository;
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
        
        int generatedAlertCount = 0;
        
        for (AlertCriteria criteria : allCriteria) {
            CriteriaEvaluation evaluation = evaluateCriteria(criteria, activeWeatherAlerts);
            generatedAlertCount += applyStateAndMaybeNotify(criteria, evaluation, true).size();
        }
        
        log.info("Generated {} alerts", generatedAlertCount);
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
        CriteriaEvaluation evaluation = evaluateCriteria(criteria, activeWeatherAlerts);
        generatedAlerts.addAll(applyStateAndMaybeNotify(criteria, evaluation, true));

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
                    saveAndPublishAlert(criteria, weatherData, false).ifPresent(alerts::add);
                }
            }
        }
        return alerts;
    }

    private CriteriaEvaluation evaluateCriteria(AlertCriteria criteria, List<WeatherData> activeWeatherAlerts) {
        if (criteria == null || !Boolean.TRUE.equals(criteria.getEnabled())) {
            return CriteriaEvaluation.notMet();
        }

        Optional<WeatherData> activeAlertMatch = activeWeatherAlerts.stream()
                .filter(weatherData -> criteriaRuleEvaluator.matches(criteria, weatherData))
                .findFirst();
        if (activeAlertMatch.isPresent()) {
            return CriteriaEvaluation.met(activeAlertMatch.get());
        }

        if (!criteriaRuleEvaluator.hasWeatherConditionRules(criteria)) {
            return CriteriaEvaluation.notMet();
        }

        if (criteria.getLatitude() == null || criteria.getLongitude() == null) {
            log.debug("Skipping condition evaluation for criteria {}: missing latitude/longitude", criteria.getId());
            return CriteriaEvaluation.notMet();
        }

        if (shouldMonitorCurrent(criteria)) {
            Optional<WeatherData> currentMatch = weatherDataPort
                    .fetchCurrentConditions(criteria.getLatitude(), criteria.getLongitude())
                    .map(current -> {
                        searchPort.indexWeatherData(current);
                        return current;
                    })
                    .filter(current -> criteriaRuleEvaluator.matches(criteria, current));
            if (currentMatch.isPresent()) {
                return CriteriaEvaluation.met(currentMatch.get());
            }
        }

        if (shouldMonitorForecast(criteria)) {
            int forecastWindowHours = normalizeForecastWindowHours(criteria.getForecastWindowHours());
            List<WeatherData> forecast = weatherDataPort.fetchForecastConditions(
                    criteria.getLatitude(),
                    criteria.getLongitude(),
                    forecastWindowHours
            );
            forecast.forEach(searchPort::indexWeatherData);
            Optional<WeatherData> forecastMatch = forecast.stream()
                    .filter(weatherData -> criteriaRuleEvaluator.matches(criteria, weatherData))
                    .findFirst();
            if (forecastMatch.isPresent()) {
                return CriteriaEvaluation.met(forecastMatch.get());
            }
        }

        return CriteriaEvaluation.notMet();
    }

    private List<Alert> applyStateAndMaybeNotify(AlertCriteria criteria, CriteriaEvaluation evaluation, boolean publish) {
        if (criteria == null || criteria.getId() == null || criteria.getId().isBlank()) {
            return List.of();
        }

        Instant now = Instant.now();
        AlertCriteriaState state = criteriaStateRepository.findByCriteriaId(criteria.getId())
                .orElseGet(() -> AlertCriteriaState.builder()
                        .criteriaId(criteria.getId())
                        .lastConditionMet(false)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

        if (!evaluation.conditionMet()) {
            if (Boolean.TRUE.equals(state.getLastConditionMet())) {
                state.setLastConditionMet(false);
                state.setUpdatedAt(now);
                criteriaStateRepository.save(state);
            }
            return List.of();
        }

        WeatherData matchedWeatherData = evaluation.matchedWeatherData();
        String eventSignature = buildEventSignature(criteria, matchedWeatherData);
        boolean wasMet = Boolean.TRUE.equals(state.getLastConditionMet());
        boolean oncePerEvent = criteria.getOncePerEvent() == null || criteria.getOncePerEvent();
        boolean signatureChanged = state.getLastEventSignature() == null || !state.getLastEventSignature().equals(eventSignature);
        boolean cooldownElapsed = isCooldownElapsed(criteria, state, now);

        boolean shouldNotify;
        if (!wasMet) {
            shouldNotify = cooldownElapsed;
        } else if (oncePerEvent) {
            shouldNotify = signatureChanged && cooldownElapsed;
        } else {
            shouldNotify = signatureChanged || cooldownElapsed;
        }

        if (shouldNotify) {
            Optional<Alert> savedAlert = saveAndPublishAlert(criteria, matchedWeatherData, publish);
            state.setLastConditionMet(true);
            state.setLastEventSignature(eventSignature);
            state.setLastNotifiedAt(now);
            state.setUpdatedAt(now);
            criteriaStateRepository.save(state);
            return savedAlert.map(List::of).orElseGet(List::of);
        }

        // Keep the state "not met" while still in cooldown after a fresh condition edge, so it can fire later.
        if (!wasMet && !cooldownElapsed) {
            state.setLastConditionMet(false);
        } else {
            state.setLastConditionMet(true);
        }
        state.setUpdatedAt(now);
        criteriaStateRepository.save(state);
        return List.of();
    }

    private boolean isCooldownElapsed(AlertCriteria criteria, AlertCriteriaState state, Instant now) {
        int rearmWindowMinutes = criteria.getRearmWindowMinutes() == null ? 0 : Math.max(criteria.getRearmWindowMinutes(), 0);
        if (rearmWindowMinutes == 0 || state.getLastNotifiedAt() == null) {
            return true;
        }
        return !state.getLastNotifiedAt().plusSeconds(rearmWindowMinutes * 60L).isAfter(now);
    }

    private String buildEventSignature(AlertCriteria criteria, WeatherData weatherData) {
        if (weatherData == null) {
            return "none";
        }
        String eventType = safeValue(weatherData.getEventType());
        if ("CURRENT_CONDITIONS".equalsIgnoreCase(eventType)) {
            return "current|" + criteria.getId();
        }
        if ("FORECAST_CONDITIONS".equalsIgnoreCase(eventType)) {
            String onset = weatherData.getOnset() != null ? weatherData.getOnset().toString() : "unknown";
            return "forecast|" + criteria.getId() + "|" + onset + "|" + safeValue(weatherData.getHeadline());
        }
        if (weatherData.getId() != null && !weatherData.getId().isBlank()) {
            return "alert|" + weatherData.getId();
        }
        return "alert|" + safeValue(weatherData.getEventType()) + "|" + safeValue(weatherData.getLocation());
    }

    private String safeValue(String value) {
        return value == null ? "unknown" : value;
    }

    private Optional<Alert> saveAndPublishAlert(AlertCriteria criteria, WeatherData weatherData, boolean publish) {
        Alert alert = createAlert(criteria, weatherData);
        Optional<Alert> existing = alertRepository.findByCriteriaIdAndEventKey(criteria.getId(), alert.getEventKey());
        if (existing.isPresent()) {
            log.debug("Skipping duplicate alert for criteria {} and eventKey {}", criteria.getId(), alert.getEventKey());
            return Optional.empty();
        }

        Alert savedAlert = alertRepository.save(alert);
        if (publish) {
            notificationPort.publishAlert(savedAlert);
        }
        log.info("Generated alert {} for user {} based on criteria {}",
                savedAlert.getId(), criteria.getUserId(), criteria.getId());
        return Optional.of(savedAlert);
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
        Instant now = Instant.now();
        String eventKey = buildEventKey(criteria, weatherData, now);
        return Alert.builder()
                .id(UUID.randomUUID().toString())
                .userId(criteria.getUserId())
                .criteriaId(criteria.getId())
                .weatherDataId(weatherData.getId())
                .eventKey(eventKey)
                .reason(buildReason(weatherData))
                .eventType(weatherData.getEventType())
                .severity(weatherData.getSeverity())
                .headline(weatherData.getHeadline())
                .description(weatherData.getDescription())
                .location(weatherData.getLocation())
                .conditionSource(resolveConditionSource(weatherData))
                .conditionOnset(weatherData.getOnset())
                .conditionExpires(weatherData.getExpires())
                .conditionTemperatureC(weatherData.getTemperature())
                .conditionPrecipitationProbability(weatherData.getPrecipitationProbability())
                .conditionPrecipitationAmount(weatherData.getPrecipitationAmount())
                .alertTime(now)
                .status(Alert.AlertStatus.PENDING)
                .build();
    }

    private String buildEventKey(AlertCriteria criteria, WeatherData weatherData, Instant now) {
        String eventType = safeValue(weatherData.getEventType());
        if ("CURRENT_CONDITIONS".equalsIgnoreCase(eventType)) {
            Instant bucket = coalesce(weatherData.getTimestamp(), weatherData.getOnset(), now).truncatedTo(ChronoUnit.HOURS);
            return "current|" + criteria.getId() + "|" + bucket;
        }
        if ("FORECAST_CONDITIONS".equalsIgnoreCase(eventType)) {
            Instant bucket = coalesce(weatherData.getOnset(), weatherData.getTimestamp(), now).truncatedTo(ChronoUnit.HOURS);
            return "forecast|" + criteria.getId() + "|" + bucket;
        }
        if (weatherData.getId() != null && !weatherData.getId().isBlank()) {
            return "alert|" + criteria.getId() + "|" + weatherData.getId();
        }
        Instant bucket = coalesce(weatherData.getOnset(), now).truncatedTo(ChronoUnit.HOURS);
        return "alert|" + criteria.getId() + "|" + eventType + "|" + bucket;
    }

    private String buildReason(WeatherData weatherData) {
        String source = resolveConditionSource(weatherData);
        if (weatherData.getHeadline() != null && !weatherData.getHeadline().isBlank()) {
            return "Matched " + source + ": " + weatherData.getHeadline();
        }
        return "Matched " + source + " conditions";
    }

    private String resolveConditionSource(WeatherData weatherData) {
        if (weatherData == null) {
            return "UNKNOWN";
        }
        if (weatherData.getStatus() != null && !weatherData.getStatus().isBlank()) {
            return weatherData.getStatus();
        }
        String eventType = weatherData.getEventType();
        if ("CURRENT_CONDITIONS".equalsIgnoreCase(eventType)) {
            return "CURRENT";
        }
        if ("FORECAST_CONDITIONS".equalsIgnoreCase(eventType)) {
            return "FORECAST";
        }
        return "ALERT";
    }

    private Instant coalesce(Instant... candidates) {
        for (Instant candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return Instant.now();
    }

    private record CriteriaEvaluation(boolean conditionMet, WeatherData matchedWeatherData) {
        static CriteriaEvaluation notMet() {
            return new CriteriaEvaluation(false, null);
        }

        static CriteriaEvaluation met(WeatherData matchedWeatherData) {
            return new CriteriaEvaluation(true, matchedWeatherData);
        }
    }
}
