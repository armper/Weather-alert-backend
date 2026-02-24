package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
import com.weather.alert.application.exception.CriteriaNotFoundException;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageAlertCriteriaUseCaseTest {
    
    @Mock
    private AlertCriteriaRepositoryPort criteriaRepository;
    
    private ManageAlertCriteriaUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new ManageAlertCriteriaUseCase(criteriaRepository);
    }
    
    @Test
    void shouldCreateCriteria() {
        // Given
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user1")
                .location("Seattle")
                .eventType("Tornado")
                .minSeverity("SEVERE")
                .build();
        
        AlertCriteria expectedCriteria = AlertCriteria.builder()
                .id("criteria1")
                .userId("user1")
                .location("Seattle")
                .eventType("Tornado")
                .minSeverity("SEVERE")
                .enabled(true)
                .build();
        
        when(criteriaRepository.save(any(AlertCriteria.class))).thenReturn(expectedCriteria);
        
        // When
        AlertCriteria result = useCase.createCriteria(request);
        
        // Then
        assertNotNull(result);
        assertEquals("user1", result.getUserId());
        assertEquals("Seattle", result.getLocation());
        assertEquals("Tornado", result.getEventType());
        assertTrue(result.getEnabled());
        verify(criteriaRepository, times(1)).save(any(AlertCriteria.class));
    }

    @Test
    void shouldApplyDefaultsForExtendedCriteriaFields() {
        // Given
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user1")
                .eventType("Rain")
                .build();

        when(criteriaRepository.save(any(AlertCriteria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AlertCriteria saved = useCase.createCriteria(request);

        // Then
        assertEquals(AlertCriteria.TemperatureUnit.F, saved.getTemperatureUnit());
        assertTrue(saved.getMonitorCurrent());
        assertTrue(saved.getMonitorForecast());
        assertEquals(48, saved.getForecastWindowHours());
        assertTrue(saved.getOncePerEvent());
        assertEquals(0, saved.getRearmWindowMinutes());
    }
    
    @Test
    void shouldDeleteCriteria() {
        // Given
        String criteriaId = "criteria1";
        when(criteriaRepository.findById(criteriaId)).thenReturn(java.util.Optional.of(AlertCriteria.builder().id(criteriaId).build()));
        
        // When
        useCase.deleteCriteria(criteriaId);
        
        // Then
        verify(criteriaRepository, times(1)).delete(criteriaId);
    }

    @Test
    void shouldThrowWhenDeletingUnknownCriteria() {
        // Given
        String criteriaId = "missing-criteria";
        when(criteriaRepository.findById(criteriaId)).thenReturn(java.util.Optional.empty());

        // When / Then
        assertThrows(CriteriaNotFoundException.class, () -> useCase.deleteCriteria(criteriaId));
        verify(criteriaRepository, never()).delete(any());
    }

    @Test
    void shouldUpdateExtendedFields() {
        // Given
        String criteriaId = "criteria-1";
        AlertCriteria existing = AlertCriteria.builder()
                .id(criteriaId)
                .userId("user1")
                .enabled(true)
                .build();

        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user1")
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .rainThreshold(40.0)
                .rainThresholdType(AlertCriteria.RainThresholdType.PROBABILITY)
                .monitorCurrent(true)
                .monitorForecast(true)
                .forecastWindowHours(48)
                .oncePerEvent(true)
                .rearmWindowMinutes(120)
                .build();

        when(criteriaRepository.findById(criteriaId)).thenReturn(java.util.Optional.of(existing));
        when(criteriaRepository.save(any(AlertCriteria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AlertCriteria result = useCase.updateCriteria(criteriaId, request);

        // Then
        assertEquals(60.0, result.getTemperatureThreshold());
        assertEquals(AlertCriteria.TemperatureDirection.BELOW, result.getTemperatureDirection());
        assertEquals(AlertCriteria.TemperatureUnit.F, result.getTemperatureUnit());
        assertEquals(40.0, result.getRainThreshold());
        assertEquals(AlertCriteria.RainThresholdType.PROBABILITY, result.getRainThresholdType());
        assertEquals(120, result.getRearmWindowMinutes());
    }
}
