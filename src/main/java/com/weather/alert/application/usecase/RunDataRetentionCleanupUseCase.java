package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.JobRunResponse;
import com.weather.alert.domain.port.AlertCriteriaStateRepositoryPort;
import com.weather.alert.domain.port.AlertDeliveryRepositoryPort;
import com.weather.alert.domain.port.AlertRepositoryPort;
import com.weather.alert.domain.port.WeatherDataSearchPort;
import com.weather.alert.infrastructure.config.DataRetentionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunDataRetentionCleanupUseCase {

    private final AlertRepositoryPort alertRepository;
    private final AlertCriteriaStateRepositoryPort criteriaStateRepository;
    private final WeatherDataSearchPort weatherDataSearchPort;
    private final AlertDeliveryRepositoryPort alertDeliveryRepository;
    private final DataRetentionProperties retentionProperties;

    public JobRunResponse run() {
        Instant startedAt = Instant.now();
        if (!retentionProperties.isEnabled()) {
            return buildResponse(startedAt, "SKIPPED", 0, 0, 0, 0, 0, "retention cleanup disabled");
        }

        int deletedAlerts = 0;
        int deletedStatesByAge = 0;
        int deletedOrphanStates = 0;
        long deletedWeatherData = 0;
        int deletedDeliveries = 0;

        if (retentionProperties.getAlertsDays() > 0) {
            try {
                Instant alertsCutoff = Instant.now().minus(Duration.ofDays(retentionProperties.getAlertsDays()));
                deletedAlerts = alertRepository.deleteByAlertTimeBefore(alertsCutoff);
            } catch (Exception ex) {
                log.error("Alert retention cleanup failed", ex);
            }
        }

        if (retentionProperties.getCriteriaStateDays() > 0) {
            try {
                Instant criteriaStateCutoff = Instant.now().minus(Duration.ofDays(retentionProperties.getCriteriaStateDays()));
                deletedStatesByAge = criteriaStateRepository.deleteByUpdatedAtBefore(criteriaStateCutoff);
            } catch (Exception ex) {
                log.error("Criteria-state age cleanup failed", ex);
            }
        }

        if (retentionProperties.isCleanupOrphanCriteriaState()) {
            try {
                deletedOrphanStates = criteriaStateRepository.deleteOrphanedStates();
            } catch (Exception ex) {
                log.error("Criteria-state orphan cleanup failed", ex);
            }
        }

        if (retentionProperties.getWeatherDataHours() > 0) {
            try {
                Instant weatherCutoff = Instant.now().minus(Duration.ofHours(retentionProperties.getWeatherDataHours()));
                deletedWeatherData = weatherDataSearchPort.deleteWeatherDataOlderThan(weatherCutoff);
            } catch (Exception ex) {
                log.error("Weather data retention cleanup failed", ex);
            }
        }

        if (retentionProperties.getDeliveryDays() > 0) {
            try {
                Instant deliveryCutoff = Instant.now().minus(Duration.ofDays(retentionProperties.getDeliveryDays()));
                deletedDeliveries = alertDeliveryRepository.deleteByCreatedAtBefore(deliveryCutoff);
            } catch (Exception ex) {
                log.error("Alert delivery retention cleanup failed", ex);
            }
        }

        return buildResponse(
                startedAt,
                "COMPLETED",
                deletedAlerts,
                deletedStatesByAge,
                deletedOrphanStates,
                deletedWeatherData,
                deletedDeliveries,
                null);
    }

    private JobRunResponse buildResponse(
            Instant startedAt,
            String status,
            int deletedAlerts,
            int deletedStatesByAge,
            int deletedOrphanStates,
            long deletedWeatherData,
            int deletedDeliveries,
            String reason) {
        Instant finishedAt = Instant.now();
        long durationMillis = Duration.between(startedAt, finishedAt).toMillis();
        Map<String, Long> metrics = new LinkedHashMap<>();
        metrics.put("alertsDeleted", (long) deletedAlerts);
        metrics.put("criteriaStateDeletedByAge", (long) deletedStatesByAge);
        metrics.put("criteriaStateDeletedOrphan", (long) deletedOrphanStates);
        metrics.put("weatherDataDeleted", deletedWeatherData);
        metrics.put("deliveriesDeleted", (long) deletedDeliveries);
        log.info(
                "Retention cleanup completed in {} ms (alertsDeleted={}, criteriaStateDeletedByAge={}, criteriaStateDeletedOrphan={}, weatherDataDeleted={}, deliveriesDeleted={})",
                durationMillis,
                deletedAlerts,
                deletedStatesByAge,
                deletedOrphanStates,
                deletedWeatherData,
                deletedDeliveries);
        return JobRunResponse.builder()
                .jobName("data-retention")
                .status(status)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .durationMillis(durationMillis)
                .message(reason)
                .metrics(metrics)
                .build();
    }
}
