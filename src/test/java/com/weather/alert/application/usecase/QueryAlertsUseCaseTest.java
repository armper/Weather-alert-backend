package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.AlertCriteriaQueryFilter;
import com.weather.alert.application.exception.AlertNotFoundException;
import com.weather.alert.application.exception.CriteriaNotFoundException;
import com.weather.alert.application.exception.InvalidAlertTransitionException;
import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.AlertRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryAlertsUseCaseTest {

    @Mock
    private AlertRepositoryPort alertRepository;

    @Mock
    private AlertCriteriaRepositoryPort criteriaRepository;

    private QueryAlertsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new QueryAlertsUseCase(alertRepository, criteriaRepository);
    }

    @Test
    void shouldThrowTypedExceptionWhenAlertNotFound() {
        when(alertRepository.findById("missing-alert")).thenReturn(Optional.empty());
        assertThrows(AlertNotFoundException.class, () -> useCase.getAlertById("missing-alert"));
    }

    @Test
    void shouldThrowTypedExceptionWhenCriteriaNotFound() {
        when(criteriaRepository.findById("missing-criteria")).thenReturn(Optional.empty());
        assertThrows(CriteriaNotFoundException.class, () -> useCase.getCriteriaById("missing-criteria"));
    }

    @Test
    void shouldReturnHistoryByCriteriaId() {
        Alert alert = Alert.builder().id("alert-1").criteriaId("criteria-1").build();
        when(alertRepository.findHistoryByCriteriaId("criteria-1")).thenReturn(List.of(alert));

        List<Alert> history = useCase.getAlertHistoryByCriteriaId("criteria-1");

        assertEquals(1, history.size());
        assertEquals("alert-1", history.get(0).getId());
    }

    @Test
    void shouldAcknowledgeAlertWhenTransitionIsValid() {
        Alert current = Alert.builder().id("alert-1").status(Alert.AlertStatus.SENT).build();
        Alert acknowledged = Alert.builder().id("alert-1").status(Alert.AlertStatus.ACKNOWLEDGED).build();
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(current));
        when(alertRepository.acknowledge(org.mockito.ArgumentMatchers.eq("alert-1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(acknowledged));

        Alert result = useCase.acknowledgeAlert("alert-1");

        assertEquals(Alert.AlertStatus.ACKNOWLEDGED, result.getStatus());
    }

    @Test
    void shouldThrowConflictWhenAcknowledgeTransitionIsInvalid() {
        Alert current = Alert.builder().id("alert-2").status(Alert.AlertStatus.PENDING).build();
        when(alertRepository.findById("alert-2")).thenReturn(Optional.of(current));
        when(alertRepository.acknowledge(org.mockito.ArgumentMatchers.eq("alert-2"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        assertThrows(InvalidAlertTransitionException.class, () -> useCase.acknowledgeAlert("alert-2"));
    }

    @Test
    void shouldExpireAlertWhenTransitionIsValid() {
        Alert current = Alert.builder().id("alert-3").status(Alert.AlertStatus.SENT).build();
        Alert expired = Alert.builder().id("alert-3").status(Alert.AlertStatus.EXPIRED).build();
        when(alertRepository.findById("alert-3")).thenReturn(Optional.of(current));
        when(alertRepository.expire(org.mockito.ArgumentMatchers.eq("alert-3"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(expired));

        Alert result = useCase.expireAlert("alert-3");

        assertEquals(Alert.AlertStatus.EXPIRED, result.getStatus());
    }

    @Test
    void shouldFilterCriteriaByTemperatureUnitAndRainRule() {
        AlertCriteria first = AlertCriteria.builder()
                .id("criteria-1")
                .userId("user-1")
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .rainThreshold(40.0)
                .rainThresholdType(AlertCriteria.RainThresholdType.PROBABILITY)
                .build();
        AlertCriteria second = AlertCriteria.builder()
                .id("criteria-2")
                .userId("user-1")
                .temperatureUnit(AlertCriteria.TemperatureUnit.C)
                .temperatureThreshold(10.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .build();
        when(criteriaRepository.findByUserId("user-1")).thenReturn(List.of(first, second));

        AlertCriteriaQueryFilter filter = AlertCriteriaQueryFilter.builder()
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .hasRainRule(true)
                .build();

        List<AlertCriteria> result = useCase.getCriteriaByUserId("user-1", filter);

        assertEquals(1, result.size());
        assertEquals("criteria-1", result.get(0).getId());
    }

    @Test
    void shouldFilterCriteriaByMonitoringFlags() {
        AlertCriteria first = AlertCriteria.builder()
                .id("criteria-1")
                .userId("user-1")
                .monitorCurrent(true)
                .monitorForecast(false)
                .build();
        AlertCriteria second = AlertCriteria.builder()
                .id("criteria-2")
                .userId("user-1")
                .monitorCurrent(true)
                .monitorForecast(true)
                .build();
        when(criteriaRepository.findByUserId("user-1")).thenReturn(List.of(first, second));

        AlertCriteriaQueryFilter filter = AlertCriteriaQueryFilter.builder()
                .monitorCurrent(true)
                .monitorForecast(true)
                .build();

        List<AlertCriteria> result = useCase.getCriteriaByUserId("user-1", filter);

        assertEquals(1, result.size());
        assertEquals("criteria-2", result.get(0).getId());
    }
}
