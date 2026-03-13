package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.JobRunResponse;
import com.weather.alert.domain.port.AlertCriteriaStateRepositoryPort;
import com.weather.alert.domain.port.AlertDeliveryRepositoryPort;
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

    @Mock
    private AlertDeliveryRepositoryPort alertDeliveryRepository;

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
        properties.setDeliveryDays(30);
        useCase = new RunDataRetentionCleanupUseCase(
                alertRepository,
                criteriaStateRepository,
                weatherDataSearchPort,
                alertDeliveryRepository,
                properties);
    }

    @Test
    void shouldRunCleanupAcrossAllStoresWhenEnabled() {
        when(alertRepository.deleteByAlertTimeBefore(any(Instant.class))).thenReturn(3);
        when(criteriaStateRepository.deleteByUpdatedAtBefore(any(Instant.class))).thenReturn(2);
        when(criteriaStateRepository.deleteOrphanedStates()).thenReturn(1);
        when(weatherDataSearchPort.deleteWeatherDataOlderThan(any(Instant.class))).thenReturn(8L);
        when(alertDeliveryRepository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(5);

        JobRunResponse response = useCase.run();

        verify(alertRepository).deleteByAlertTimeBefore(any(Instant.class));
        verify(criteriaStateRepository).deleteByUpdatedAtBefore(any(Instant.class));
        verify(criteriaStateRepository).deleteOrphanedStates();
        verify(weatherDataSearchPort).deleteWeatherDataOlderThan(any(Instant.class));
        verify(alertDeliveryRepository).deleteByCreatedAtBefore(any(Instant.class));
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(3L, response.getMetrics().get("alertsDeleted"));
        assertEquals(8L, response.getMetrics().get("weatherDataDeleted"));
        assertEquals(5L, response.getMetrics().get("deliveriesDeleted"));
    }

    @Test
    void shouldSkipCleanupWhenDisabled() {
        properties.setEnabled(false);

        JobRunResponse response = useCase.run();

        verify(alertRepository, never()).deleteByAlertTimeBefore(any(Instant.class));
        verify(criteriaStateRepository, never()).deleteByUpdatedAtBefore(any(Instant.class));
        verify(criteriaStateRepository, never()).deleteOrphanedStates();
        verify(weatherDataSearchPort, never()).deleteWeatherDataOlderThan(any(Instant.class));
        verify(alertDeliveryRepository, never()).deleteByCreatedAtBefore(any(Instant.class));
        assertEquals("SKIPPED", response.getStatus());
        assertEquals("retention cleanup disabled", response.getMessage());
    }

    @Test
    void shouldSkipDeliveryCleanupWhenDeliveryDaysIsZero() {
        properties.setDeliveryDays(0);
        when(alertRepository.deleteByAlertTimeBefore(any(Instant.class))).thenReturn(0);
        when(criteriaStateRepository.deleteByUpdatedAtBefore(any(Instant.class))).thenReturn(0);
        when(criteriaStateRepository.deleteOrphanedStates()).thenReturn(0);
        when(weatherDataSearchPort.deleteWeatherDataOlderThan(any(Instant.class))).thenReturn(0L);

        JobRunResponse response = useCase.run();

        verify(alertDeliveryRepository, never()).deleteByCreatedAtBefore(any(Instant.class));
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(0L, response.getMetrics().get("deliveriesDeleted"));
    }
}
