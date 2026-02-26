package com.weather.alert.infrastructure.config;

import com.weather.alert.domain.port.WeatherDataSearchPort;
import com.weather.alert.infrastructure.adapter.persistence.JpaAlertCriteriaStateRepository;
import com.weather.alert.infrastructure.adapter.persistence.JpaAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataRetentionSchedulerTest {

    @Mock
    private JpaAlertRepository alertRepository;

    @Mock
    private JpaAlertCriteriaStateRepository criteriaStateRepository;

    @Mock
    private WeatherDataSearchPort weatherDataSearchPort;

    @Test
    void shouldRunCleanupAcrossAllStoresWhenEnabled() {
        DataRetentionProperties properties = new DataRetentionProperties();
        properties.setEnabled(true);
        properties.setAlertsDays(2);
        properties.setWeatherDataHours(72);
        properties.setCriteriaStateDays(14);
        properties.setCleanupOrphanCriteriaState(true);

        when(alertRepository.deleteByAlertTimeBefore(any(Instant.class))).thenReturn(3);
        when(criteriaStateRepository.deleteByUpdatedAtBefore(any(Instant.class))).thenReturn(2);
        when(criteriaStateRepository.deleteOrphanedStates()).thenReturn(1);
        when(weatherDataSearchPort.deleteWeatherDataOlderThan(any(Instant.class))).thenReturn(8L);

        DataRetentionScheduler scheduler = new DataRetentionScheduler(
                alertRepository,
                criteriaStateRepository,
                weatherDataSearchPort,
                properties);

        scheduler.cleanupOldData();

        verify(alertRepository).deleteByAlertTimeBefore(any(Instant.class));
        verify(criteriaStateRepository).deleteByUpdatedAtBefore(any(Instant.class));
        verify(criteriaStateRepository).deleteOrphanedStates();
        verify(weatherDataSearchPort).deleteWeatherDataOlderThan(any(Instant.class));
    }

    @Test
    void shouldSkipCleanupWhenDisabled() {
        DataRetentionProperties properties = new DataRetentionProperties();
        properties.setEnabled(false);

        DataRetentionScheduler scheduler = new DataRetentionScheduler(
                alertRepository,
                criteriaStateRepository,
                weatherDataSearchPort,
                properties);

        scheduler.cleanupOldData();

        verify(alertRepository, never()).deleteByAlertTimeBefore(any(Instant.class));
        verify(criteriaStateRepository, never()).deleteByUpdatedAtBefore(any(Instant.class));
        verify(criteriaStateRepository, never()).deleteOrphanedStates();
        verify(weatherDataSearchPort, never()).deleteWeatherDataOlderThan(any(Instant.class));
    }
}
