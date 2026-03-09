package com.weather.alert.infrastructure.config;

import com.weather.alert.application.usecase.RunDataRetentionCleanupUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataRetentionSchedulerTest {

    @Mock
    private RunDataRetentionCleanupUseCase runDataRetentionCleanupUseCase;

    @Test
    void shouldDelegateToUseCase() {
        DataRetentionScheduler scheduler = new DataRetentionScheduler(runDataRetentionCleanupUseCase);

        scheduler.cleanupOldData();

        verify(runDataRetentionCleanupUseCase).run();
    }
}
