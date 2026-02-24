package com.weather.alert.application.usecase;

import com.weather.alert.application.exception.AlertNotFoundException;
import com.weather.alert.application.exception.CriteriaNotFoundException;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.AlertRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
}
