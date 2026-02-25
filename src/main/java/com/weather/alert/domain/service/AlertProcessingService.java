package com.weather.alert.domain.service;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.AlertCriteriaState;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.AlertCriteriaStateRepositoryPort;
import com.weather.alert.domain.port.AlertRepositoryPort;
import com.weather.alert.domain.port.NotificationPort;
import com.weather.alert.domain.port.WeatherDataPort;
import com.weather.alert.domain.port.WeatherDataSearchPort;
import com.weather.alert.domain.port.WeatherFetchResult;
import com.weather.alert.domain.service.evaluation.AlertCriteriaRuleEvaluator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
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

    private static final int CRITERIA_BATCH_SIZE = 100;

    private final WeatherDataPort weatherDataPort;
    private final AlertCriteriaRepositoryPort criteriaRepository;
    private final AlertRepositoryPort alertRepository;
    private final NotificationPort notificationPort;
    private final WeatherDataSearchPort searchPort;
    private final AlertCriteriaStateRepositoryPort criteriaStateRepository;
    private final AlertCriteriaRuleEvaluator criteriaRuleEvaluator;
    private final MeterRegistry meterRegistry;

    /**
     * Process weather data and generate alerts based on user criteria
     */
    public void processWeatherAlerts() {
        Timer.Sample processingTimer = Timer.start(meterRegistry);
        try {
            log.info("Starting weather alert processing");

            WeatherFetchResult<List<WeatherData>> activeAlertsResult = weatherDataPort.fetchActiveAlertsWithStatus();
            List<WeatherData> activeWeatherAlerts = activeAlertsResult.data() == null ? List.of() : activeAlertsResult.data();
            log.info(
                    "Fetched {} active NOAA weather alerts (providerSuccess={})",
                    activeWeatherAlerts.size(),
                    activeAlertsResult.successful());
            if (!activeAlertsResult.successful()) {
                log.warn("NOAA active alerts unavailable. reason={}", safeValue(activeAlertsResult.failureReason()));
            }

            activeWeatherAlerts.forEach(searchPort::indexWeatherData);

            List<AlertCriteria> allCriteria = criteriaRepository.findAllEnabled();
            log.info("Found {} enabled alert criteria", allCriteria.size());

            int generatedAlertCount = 0;
            int metCount = 0;
            int notMetCount = 0;
            int unavailableCount = 0;
            int suppressedCount = 0;

            HashMap<CoordinateKey, WeatherFetchResult<Optional<WeatherData>>> currentConditionsCache = new HashMap<>();
            HashMap<ForecastKey, WeatherFetchResult<List<WeatherData>>> forecastConditionsCache = new HashMap<>();

            List<List<AlertCriteria>> batches = partition(allCriteria, CRITERIA_BATCH_SIZE);
            for (int i = 0; i < batches.size(); i++) {
                List<AlertCriteria> batch = batches.get(i);
                log.info("Processing criteria batch {}/{} (size={})", i + 1, batches.size(), batch.size());

                for (AlertCriteria criteria : batch) {
                    meterRegistry.counter("weather.alert.criteria.evaluated").increment();

                    CriteriaEvaluation evaluation = evaluateCriteria(
                            criteria,
                            activeWeatherAlerts,
                            activeAlertsResult.successful(),
                            activeAlertsResult.failureReason(),
                            currentConditionsCache,
                            forecastConditionsCache);

                    switch (evaluation.status()) {
                        case MET -> {
                            metCount++;
                            meterRegistry.counter("weather.alert.criteria.met").increment();
                        }
                        case NOT_MET -> {
                            notMetCount++;
                            meterRegistry.counter("weather.alert.criteria.not_met").increment();
                        }
                        case UNAVAILABLE -> {
                            unavailableCount++;
                            meterRegistry.counter("weather.alert.criteria.unavailable").increment();
                        }
                    }

                    List<Alert> generatedAlerts = applyStateAndMaybeNotify(criteria, evaluation, true);
                    if (evaluation.status() == CriteriaEvaluationStatus.MET && generatedAlerts.isEmpty()) {
                        suppressedCount++;
                        meterRegistry.counter("weather.alert.criteria.suppressed").increment();
                    }
                    if (!generatedAlerts.isEmpty()) {
                        meterRegistry.counter("weather.alert.triggered").increment(generatedAlerts.size());
                    }
                    generatedAlertCount += generatedAlerts.size();
                }
            }

            log.info(
                    "Weather alert processing completed: generated={}, met={}, notMet={}, unavailable={}, suppressed={}",
                    generatedAlertCount,
                    metCount,
                    notMetCount,
                    unavailableCount,
                    suppressedCount);
        } finally {
            processingTimer.stop(meterRegistry.timer("weather.alert.processing.duration"));
        }
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

        WeatherFetchResult<List<WeatherData>> activeAlertsResult = weatherDataPort.fetchActiveAlertsWithStatus();
        List<WeatherData> activeWeatherAlerts = activeAlertsResult.data() == null ? List.of() : activeAlertsResult.data();
        activeWeatherAlerts.forEach(searchPort::indexWeatherData);

        HashMap<CoordinateKey, WeatherFetchResult<Optional<WeatherData>>> currentConditionsCache = new HashMap<>();
        HashMap<ForecastKey, WeatherFetchResult<List<WeatherData>>> forecastConditionsCache = new HashMap<>();

        CriteriaEvaluation evaluation = evaluateCriteria(
                criteria,
                activeWeatherAlerts,
                activeAlertsResult.successful(),
                activeAlertsResult.failureReason(),
                currentConditionsCache,
                forecastConditionsCache);
        List<Alert> generatedAlerts = applyStateAndMaybeNotify(criteria, evaluation, true);

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

    private CriteriaEvaluation evaluateCriteria(
            AlertCriteria criteria,
            List<WeatherData> activeWeatherAlerts,
            boolean activeAlertsSuccessful,
            String activeAlertsFailureReason,
            HashMap<CoordinateKey, WeatherFetchResult<Optional<WeatherData>>> currentConditionsCache,
            HashMap<ForecastKey, WeatherFetchResult<List<WeatherData>>> forecastConditionsCache) {
        if (criteria == null || !Boolean.TRUE.equals(criteria.getEnabled())) {
            return CriteriaEvaluation.notMet("criteria disabled");
        }

        Optional<WeatherData> activeAlertMatch = activeWeatherAlerts.stream()
                .filter(weatherData -> criteriaRuleEvaluator.matches(criteria, weatherData))
                .findFirst();
        if (activeAlertMatch.isPresent()) {
            return CriteriaEvaluation.met(activeAlertMatch.get(), "active alert match");
        }

        boolean hasConditionRules = criteriaRuleEvaluator.hasWeatherConditionRules(criteria);
        if (!hasConditionRules) {
            if (!activeAlertsSuccessful) {
                return CriteriaEvaluation.unavailable("active alerts unavailable: " + safeValue(activeAlertsFailureReason));
            }
            return CriteriaEvaluation.notMet("no active alert match");
        }

        if (criteria.getLatitude() == null || criteria.getLongitude() == null) {
            log.debug("Skipping condition evaluation for criteria {}: missing latitude/longitude", criteria.getId());
            return CriteriaEvaluation.notMet("condition rules configured without coordinates");
        }

        List<String> unavailableReasons = new ArrayList<>();
        if (!activeAlertsSuccessful) {
            unavailableReasons.add("active alerts unavailable");
        }

        if (shouldMonitorCurrent(criteria)) {
            CoordinateKey key = new CoordinateKey(criteria.getLatitude(), criteria.getLongitude());
            WeatherFetchResult<Optional<WeatherData>> currentResult = currentConditionsCache.computeIfAbsent(
                    key,
                    coordinateKey -> weatherDataPort.fetchCurrentConditionsWithStatus(coordinateKey.latitude(), coordinateKey.longitude()));
            if (!currentResult.successful()) {
                unavailableReasons.add("current conditions unavailable: " + safeValue(currentResult.failureReason()));
            } else {
                Optional<WeatherData> current = currentResult.data() == null ? Optional.empty() : currentResult.data();
                current.ifPresent(searchPort::indexWeatherData);
                if (current.isPresent() && criteriaRuleEvaluator.matches(criteria, current.get())) {
                    return CriteriaEvaluation.met(current.get(), "current conditions match");
                }
            }
        }

        if (shouldMonitorForecast(criteria)) {
            int forecastWindowHours = normalizeForecastWindowHours(criteria.getForecastWindowHours());
            ForecastKey key = new ForecastKey(criteria.getLatitude(), criteria.getLongitude(), forecastWindowHours);
            WeatherFetchResult<List<WeatherData>> forecastResult = forecastConditionsCache.computeIfAbsent(
                    key,
                    forecastKey -> weatherDataPort.fetchForecastConditionsWithStatus(
                            forecastKey.latitude(),
                            forecastKey.longitude(),
                            forecastKey.windowHours()));

            if (!forecastResult.successful()) {
                unavailableReasons.add("forecast unavailable: " + safeValue(forecastResult.failureReason()));
            } else {
                List<WeatherData> forecast = forecastResult.data() == null ? List.of() : forecastResult.data();
                forecast.forEach(searchPort::indexWeatherData);
                Optional<WeatherData> forecastMatch = forecast.stream()
                        .filter(weatherData -> criteriaRuleEvaluator.matches(criteria, weatherData))
                        .findFirst();
                if (forecastMatch.isPresent()) {
                    return CriteriaEvaluation.met(forecastMatch.get(), "forecast match");
                }
            }
        }

        if (!unavailableReasons.isEmpty()) {
            return CriteriaEvaluation.unavailable(String.join("; ", unavailableReasons));
        }
        return CriteriaEvaluation.notMet("no condition match");
    }

    private List<Alert> applyStateAndMaybeNotify(AlertCriteria criteria, CriteriaEvaluation evaluation, boolean publish) {
        if (criteria == null || criteria.getId() == null || criteria.getId().isBlank()) {
            return List.of();
        }

        if (evaluation.status() == CriteriaEvaluationStatus.UNAVAILABLE) {
            log.warn(
                    "Skipping state transition for criteria {} due to unavailable data. reason={}",
                    criteria.getId(),
                    safeValue(evaluation.reason()));
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
            log.debug("Criteria {} evaluated outcome=NOT_MET reason={}", criteria.getId(), safeValue(evaluation.reason()));
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
            log.info(
                    "Criteria decision outcome=TRIGGERED criteriaId={} eventSignature={}",
                    criteria.getId(),
                    eventSignature);
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
        log.info(
                "Criteria decision outcome=SUPPRESSED criteriaId={} eventSignature={} oncePerEvent={} signatureChanged={} cooldownElapsed={}",
                criteria.getId(),
                eventSignature,
                oncePerEvent,
                signatureChanged,
                cooldownElapsed);
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
            meterRegistry.counter("weather.alert.criteria.deduped").increment();
            log.info("Criteria decision outcome=DEDUPED criteriaId={} eventKey={}", criteria.getId(), alert.getEventKey());
            return Optional.empty();
        }

        Alert savedAlert = alertRepository.save(alert);
        if (publish) {
            notificationPort.publishAlert(savedAlert);
        }
        log.info(
                "Generated alert {} for user {} based on criteria {} (eventKey={})",
                savedAlert.getId(),
                criteria.getUserId(),
                criteria.getId(),
                savedAlert.getEventKey());
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

    private List<List<AlertCriteria>> partition(List<AlertCriteria> criteria, int batchSize) {
        if (criteria == null || criteria.isEmpty()) {
            return List.of();
        }
        List<List<AlertCriteria>> partitions = new ArrayList<>();
        for (int start = 0; start < criteria.size(); start += batchSize) {
            int end = Math.min(start + batchSize, criteria.size());
            partitions.add(criteria.subList(start, end));
        }
        return partitions;
    }

    private enum CriteriaEvaluationStatus {
        MET,
        NOT_MET,
        UNAVAILABLE
    }

    private record CriteriaEvaluation(
            CriteriaEvaluationStatus status,
            WeatherData matchedWeatherData,
            String reason) {

        static CriteriaEvaluation notMet(String reason) {
            return new CriteriaEvaluation(CriteriaEvaluationStatus.NOT_MET, null, reason);
        }

        static CriteriaEvaluation met(WeatherData matchedWeatherData, String reason) {
            return new CriteriaEvaluation(CriteriaEvaluationStatus.MET, matchedWeatherData, reason);
        }

        static CriteriaEvaluation unavailable(String reason) {
            return new CriteriaEvaluation(CriteriaEvaluationStatus.UNAVAILABLE, null, reason);
        }

        boolean conditionMet() {
            return status == CriteriaEvaluationStatus.MET;
        }
    }

    private record CoordinateKey(double latitude, double longitude) {
    }

    private record ForecastKey(double latitude, double longitude, int windowHours) {
    }
}
