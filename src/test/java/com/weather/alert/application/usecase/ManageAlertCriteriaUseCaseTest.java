package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
import com.weather.alert.application.exception.CriteriaNotFoundException;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.UserApprovalStatus;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.EmailSenderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import com.weather.alert.domain.service.AlertProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageAlertCriteriaUseCaseTest {
    
    @Mock
    private AlertCriteriaRepositoryPort criteriaRepository;

    @Mock
    private AlertProcessingService alertProcessingService;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private EmailSenderPort emailSenderPort;
    
    private ManageAlertCriteriaUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new ManageAlertCriteriaUseCase(criteriaRepository, alertProcessingService, userRepository, emailSenderPort);
    }
    
    @Test
    void shouldCreateCriteria() {
        // Given
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user1")
                .name("Seattle Tornado Watch")
                .location("Seattle")
                .eventType("Tornado")
                .minSeverity("SEVERE")
                .build();
        
        AlertCriteria expectedCriteria = AlertCriteria.builder()
                .id("criteria1")
                .userId("user1")
                .name("Seattle Tornado Watch")
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
        assertEquals("Seattle Tornado Watch", result.getName());
        assertEquals("Seattle", result.getLocation());
        assertEquals("Tornado", result.getEventType());
        assertTrue(result.getEnabled());
        verify(criteriaRepository, times(1)).save(any(AlertCriteria.class));
        verify(alertProcessingService, times(1)).processCriteriaImmediately(expectedCriteria);
    }

    @Test
    void shouldSendConfirmationEmailWhenEnabledAndUserEligible() {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user1")
                .name("Annoying Winds")
                .location("Orlando")
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .build();

        AlertCriteria saved = AlertCriteria.builder()
                .id("criteria1")
                .userId("user1")
                .name("Annoying Winds")
                .location("Orlando")
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .enabled(true)
                .build();

        when(criteriaRepository.save(any(AlertCriteria.class))).thenReturn(saved);
        when(userRepository.findById("user1")).thenReturn(Optional.of(User.builder()
                .id("user1")
                .email("user1@example.com")
                .approvalStatus(UserApprovalStatus.ACTIVE)
                .emailVerified(true)
                .emailEnabled(true)
                .build()));
        ReflectionTestUtils.setField(useCase, "sendCriteriaCreatedEmail", true);
        ReflectionTestUtils.setField(useCase, "criteriaCreatedEmailSubject", "Criteria Created");

        useCase.createCriteria(request);

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSenderPort, times(1)).send(emailCaptor.capture());
        EmailMessage sent = emailCaptor.getValue();
        assertNotNull(sent);
        assertNotNull(sent.body());
        assertTrue(sent.body().contains("Alert name: Annoying Winds"));
        assertTrue(sent.body().contains("temperature is below 60 F"));
        assertTrue(sent.body().contains("Area: Orlando"));
        assertFalse(sent.body().contains("criteria1"));
        assertFalse(sent.body().contains("user1"));
    }

    @Test
    void shouldNotFailWhenConfirmationEmailDeliveryFails() {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user1")
                .location("Orlando")
                .build();
        AlertCriteria saved = AlertCriteria.builder()
                .id("criteria1")
                .userId("user1")
                .location("Orlando")
                .enabled(true)
                .build();
        when(criteriaRepository.save(any(AlertCriteria.class))).thenReturn(saved);
        when(userRepository.findById("user1")).thenReturn(Optional.of(User.builder()
                .id("user1")
                .email("user1@example.com")
                .approvalStatus(UserApprovalStatus.ACTIVE)
                .emailVerified(true)
                .emailEnabled(true)
                .build()));
        doThrow(new RuntimeException("smtp down")).when(emailSenderPort).send(any(EmailMessage.class));
        ReflectionTestUtils.setField(useCase, "sendCriteriaCreatedEmail", true);
        ReflectionTestUtils.setField(useCase, "criteriaCreatedEmailSubject", "Criteria Created");

        AlertCriteria result = useCase.createCriteria(request);

        assertNotNull(result);
        assertEquals("criteria1", result.getId());
        verify(emailSenderPort, times(1)).send(any(EmailMessage.class));
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
        verify(alertProcessingService, times(1)).processCriteriaImmediately(saved);
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
    void shouldSendDeletionEmailWhenEnabledAndUserEligible() {
        String criteriaId = "criteria1";
        AlertCriteria criteria = AlertCriteria.builder()
                .id(criteriaId)
                .userId("user1")
                .name("Annoying Winds")
                .location("Orlando")
                .build();
        when(criteriaRepository.findById(criteriaId)).thenReturn(Optional.of(criteria));
        when(userRepository.findById("user1")).thenReturn(Optional.of(User.builder()
                .id("user1")
                .email("user1@example.com")
                .approvalStatus(UserApprovalStatus.ACTIVE)
                .emailVerified(true)
                .emailEnabled(true)
                .build()));
        ReflectionTestUtils.setField(useCase, "sendCriteriaDeletedEmail", true);
        ReflectionTestUtils.setField(useCase, "criteriaDeletedEmailSubject", "Criteria Deleted");

        useCase.deleteCriteria(criteriaId);

        verify(criteriaRepository, times(1)).delete(criteriaId);
        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSenderPort, times(1)).send(emailCaptor.capture());
        EmailMessage sent = emailCaptor.getValue();
        assertNotNull(sent);
        assertNotNull(sent.body());
        assertTrue(sent.body().contains("Alert name: Annoying Winds"));
        assertTrue(sent.body().contains("Area: Orlando"));
        assertFalse(sent.body().contains("criteria1"));
        assertFalse(sent.body().contains("user1"));
    }

    @Test
    void shouldNormalizeBlankNameToNull() {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user1")
                .name("   ")
                .location("Orlando")
                .build();
        when(criteriaRepository.save(any(AlertCriteria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertCriteria saved = useCase.createCriteria(request);

        assertNull(saved.getName());
    }

    @Test
    void shouldNotFailWhenDeletionEmailDeliveryFails() {
        String criteriaId = "criteria1";
        AlertCriteria criteria = AlertCriteria.builder()
                .id(criteriaId)
                .userId("user1")
                .location("Orlando")
                .build();
        when(criteriaRepository.findById(criteriaId)).thenReturn(Optional.of(criteria));
        when(userRepository.findById("user1")).thenReturn(Optional.of(User.builder()
                .id("user1")
                .email("user1@example.com")
                .approvalStatus(UserApprovalStatus.ACTIVE)
                .emailVerified(true)
                .emailEnabled(true)
                .build()));
        doThrow(new RuntimeException("smtp down")).when(emailSenderPort).send(any(EmailMessage.class));
        ReflectionTestUtils.setField(useCase, "sendCriteriaDeletedEmail", true);
        ReflectionTestUtils.setField(useCase, "criteriaDeletedEmailSubject", "Criteria Deleted");

        assertDoesNotThrow(() -> useCase.deleteCriteria(criteriaId));
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
