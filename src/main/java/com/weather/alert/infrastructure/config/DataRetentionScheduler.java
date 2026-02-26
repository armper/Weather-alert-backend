package com.weather.alert.infrastructure.config;

import com.weather.alert.domain.port.WeatherDataSearchPort;
import com.weather.alert.infrastructure.adapter.persistence.JpaAlertCriteriaStateRepository;
import com.weather.alert.infrastructure.adapter.persistence.JpaAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataRetentionScheduler {

    private final JpaAlertRepository alertRepository;
    private final JpaAlertCriteriaStateRepository criteriaStateRepository;
    private final WeatherDataSearchPort weatherDataSearchPort;
    private final DataRetentionProperties retentionProperties;

    @Scheduled(
            fixedDelayString = "${app.retention.cleanup-fixed-delay-ms:3600000}",
            initialDelayString = "${app.retention.cleanup-initial-delay-ms:120000}")
    public void cleanupOldData() {
        if (!retentionProperties.isEnabled()) {
            return;
        }

        Instant startedAt = Instant.now();
        int deletedAlerts = 0;
        int deletedStatesByAge = 0;
        int deletedOrphanStates = 0;
        long deletedWeatherDocs = 0;

        if (retentionProperties.getAlertsDays() > 0) {
            try {
                Instant alertsCutoff = Instant.now().minus(Duration.ofDays(retentionProperties.getAlertsDays()));
                deletedAlerts = alertRepository.deleteByAlertTimeBefore(alertsCutoff);
            } catch (Exception e) {
                log.error("Alert retention cleanup failed", e);
            }
        }

        if (retentionProperties.getCriteriaStateDays() > 0) {
            try {
                Instant criteriaStateCutoff = Instant.now()
                        .minus(Duration.ofDays(retentionProperties.getCriteriaStateDays()));
                deletedStatesByAge = criteriaStateRepository.deleteByUpdatedAtBefore(criteriaStateCutoff);
            } catch (Exception e) {
                log.error("Criteria-state age cleanup failed", e);
            }
        }

        if (retentionProperties.isCleanupOrphanCriteriaState()) {
            try {
                deletedOrphanStates = criteriaStateRepository.deleteOrphanedStates();
            } catch (Exception e) {
                log.error("Criteria-state orphan cleanup failed", e);
            }
        }

        if (retentionProperties.getWeatherDataHours() > 0) {
            try {
                Instant weatherCutoff = Instant.now().minus(Duration.ofHours(retentionProperties.getWeatherDataHours()));
                deletedWeatherDocs = weatherDataSearchPort.deleteWeatherDataOlderThan(weatherCutoff);
            } catch (Exception e) {
                log.error("Weather index retention cleanup failed", e);
            }
        }

        log.info(
                "Retention cleanup completed in {} ms (alertsDeleted={}, criteriaStateDeletedByAge={}, criteriaStateDeletedOrphan={}, weatherDocsDeleted={})",
                Duration.between(startedAt, Instant.now()).toMillis(),
                deletedAlerts,
                deletedStatesByAge,
                deletedOrphanStates,
                deletedWeatherDocs);
    }
}
