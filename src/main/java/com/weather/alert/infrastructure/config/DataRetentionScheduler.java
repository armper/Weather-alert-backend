package com.weather.alert.infrastructure.config;

import com.weather.alert.application.usecase.RunDataRetentionCleanupUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        value = "app.retention.schedule-enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class DataRetentionScheduler {

    private final RunDataRetentionCleanupUseCase runDataRetentionCleanupUseCase;

    @Scheduled(
            fixedDelayString = "${app.retention.cleanup-fixed-delay-ms:3600000}",
            initialDelayString = "${app.retention.cleanup-initial-delay-ms:120000}")
    public void cleanupOldData() {
        runDataRetentionCleanupUseCase.run();
    }
}
