package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.JobRunResponse;
import com.weather.alert.domain.port.AlertCriteriaStateRepositoryPort;
import com.weather.alert.domain.port.AlertRepositoryPort;
import com.weather.alert.domain.port.WeatherDataSearchPort;
import com.weather.alert.infrastructure.config.DataRetentionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunDataRetentionCleanupUseCaseTest {

    @Mock
    private AlertRepositoryPort alertRepository;

    @Mock
    private AlertCriteriaStateRepositoryPort criteriaStateRepository;

    @Mock
    private WeatherDataSearchPort weatherDataSearchPort;

    private DataRetentionProperties properties;
    private RunDataRetentionCleanupUseCase useCase;

    @BeforeEach
    void setUp() {
        properties = new DataRetentionProperties();
        properties.setEnabled(true);
        properties.setAlertsDays(2);
        properties.setWeatherDataHours(72);
        properties.setCriteriaStateDays(14);
        properties.setCleanupOrphanCriteriaState(true);
        useCase = new RunDataRetentionCleanupUseCase(
                alertRepository,
                criteriaStateRepository,
                weatherDataSearchPort,
                properties);
    }

    @Test
    void shouldRunCleanupAcrossAllStoresWhenEnabled() {
        when(alertRepository.deleteByAlertTimeBefore(any(Instant.class))).thenReturn(3);
        when(criteriaStateRepository.deleteByUpdatedAtBefore(any(Instant.class))).thenReturn(2);
        when(criteriaStateRepository.deleteOrphanedStates()).thenReturn(1);
        when(weatherDataSearchPort.deleteWeatherDataOlderThan(any(Instant.class))).thenReturn(8L);

        JobRunResponse response = useCase.run();

        verify(alertRepository).deleteByAlertTimeBefore(any(Instant.class));
        verify(criteriaStateRepository).deleteByUpdatedAtBefore(any(Instant.class));
        verify(criteriaStateRepository).deleteOrphanedStates();
        verify(weatherDataSearchPort).deleteWeatherDataOlderThan(any(Instant.class));
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(3L, response.getMetrics().get("alertsDeleted"));
        assertEquals(8L, response.getMetrics().get("weatherDataDeleted"));
    }

    @Test
    void shouldSkipCleanupWhenDisabled() {
        properties.setEnabled(false);

        JobRunResponse response = useCase.run();

        verify(alertRepository, never()).deleteByAlertTimeBefore(any(Instant.class));
        verify(criteriaStateRepository, never()).deleteByUpdatedAtBefore(any(Instant.class));
        verify(criteriaStateRepository, never()).deleteOrphanedStates();
        verify(weatherDataSearchPort, never()).deleteWeatherDataOlderThan(any(Instant.class));
        assertEquals("SKIPPED", response.getStatus());
        assertEquals("retention cleanup disabled", response.getMessage());
    }
}
