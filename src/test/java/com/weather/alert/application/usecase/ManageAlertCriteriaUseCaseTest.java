package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
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
    void shouldDeleteCriteria() {
        // Given
        String criteriaId = "criteria1";
        
        // When
        useCase.deleteCriteria(criteriaId);
        
        // Then
        verify(criteriaRepository, times(1)).delete(criteriaId);
    }
}
