package com.weather.alert.application.usecase;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.AlertDeliveryStatus;
import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.model.EmailSendResult;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.port.AlertDeliveryDlqPublisherPort;
import com.weather.alert.domain.port.AlertDeliveryRepositoryPort;
import com.weather.alert.domain.port.AlertRepositoryPort;
import com.weather.alert.domain.port.EmailSenderPort;
import com.weather.alert.domain.service.notification.EmailDeliveryException;
import com.weather.alert.infrastructure.config.NotificationDeliveryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessAlertDeliveryTaskUseCaseTest {

    @Mock
    private AlertDeliveryRepositoryPort alertDeliveryRepository;

    @Mock
    private AlertRepositoryPort alertRepository;

    @Mock
    private EmailSenderPort emailSenderPort;

    @Mock
    private AlertDeliveryDlqPublisherPort dlqPublisher;

    private ProcessAlertDeliveryTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        NotificationDeliveryProperties properties = new NotificationDeliveryProperties();
        properties.setMaxAttempts(3);
        properties.setRetryBaseSeconds(10);
        properties.setRetryMaxSeconds(60);
        useCase = new ProcessAlertDeliveryTaskUseCase(
                alertDeliveryRepository,
                alertRepository,
                emailSenderPort,
                dlqPublisher,
                properties);
    }

    @Test
    void shouldMarkSentWhenEmailDeliverySucceeds() {
        AlertDeliveryRecord delivery = pending("delivery-1", 0);
        when(alertDeliveryRepository.findById("delivery-1")).thenReturn(Optional.of(delivery));
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(Alert.builder()
                .id("alert-1")
                .headline("Rain incoming")
                .build()));
        when(emailSenderPort.send(any())).thenReturn(new EmailSendResult("provider-id-1"));

        useCase.processTask("delivery-1");

        ArgumentCaptor<AlertDeliveryRecord> captor = ArgumentCaptor.forClass(AlertDeliveryRecord.class);
        verify(alertDeliveryRepository, atLeast(2)).save(captor.capture());
        AlertDeliveryRecord finalState = captor.getValue();
        assertEquals(AlertDeliveryStatus.SENT, finalState.getStatus());
        assertEquals(1, finalState.getAttemptCount());
        assertEquals("provider-id-1", finalState.getProviderMessageId());
        assertNotNull(finalState.getSentAt());
        verify(alertRepository).markAsSent("alert-1", finalState.getSentAt());
        verify(dlqPublisher, never()).publishFailure(any(), any(), any());
    }

    @Test
    void shouldScheduleRetryForRetryableFailure() {
        AlertDeliveryRecord delivery = pending("delivery-1", 0);
        when(alertDeliveryRepository.findById("delivery-1")).thenReturn(Optional.of(delivery));
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(Alert.builder().id("alert-1").build()));
        when(emailSenderPort.send(any())).thenThrow(new EmailDeliveryException(
                DeliveryFailureType.RETRYABLE,
                "smtp timeout",
                null));

        useCase.processTask("delivery-1");

        ArgumentCaptor<AlertDeliveryRecord> captor = ArgumentCaptor.forClass(AlertDeliveryRecord.class);
        verify(alertDeliveryRepository, atLeast(2)).save(captor.capture());
        AlertDeliveryRecord finalState = captor.getValue();
        assertEquals(AlertDeliveryStatus.RETRY_SCHEDULED, finalState.getStatus());
        assertEquals(1, finalState.getAttemptCount());
        assertNotNull(finalState.getNextAttemptAt());
        assertTrue(finalState.getNextAttemptAt().isAfter(Instant.now().minusSeconds(1)));
        verify(dlqPublisher, never()).publishFailure(any(), any(), any());
    }

    @Test
    void shouldMarkFailedAndPublishDlqForNonRetryableFailure() {
        AlertDeliveryRecord delivery = pending("delivery-1", 0);
        when(alertDeliveryRepository.findById("delivery-1")).thenReturn(Optional.of(delivery));
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(Alert.builder().id("alert-1").build()));
        when(emailSenderPort.send(any())).thenThrow(new EmailDeliveryException(
                DeliveryFailureType.NON_RETRYABLE,
                "invalid email",
                null));

        useCase.processTask("delivery-1");

        ArgumentCaptor<AlertDeliveryRecord> captor = ArgumentCaptor.forClass(AlertDeliveryRecord.class);
        verify(alertDeliveryRepository, atLeast(2)).save(captor.capture());
        AlertDeliveryRecord finalState = captor.getValue();
        assertEquals(AlertDeliveryStatus.FAILED, finalState.getStatus());
        assertEquals(1, finalState.getAttemptCount());
        verify(dlqPublisher).publishFailure(any(AlertDeliveryRecord.class), any(), any());
    }

    @Test
    void shouldMarkFailedWhenMaxAttemptsReached() {
        AlertDeliveryRecord delivery = pending("delivery-1", 2);
        when(alertDeliveryRepository.findById("delivery-1")).thenReturn(Optional.of(delivery));
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(Alert.builder().id("alert-1").build()));
        when(emailSenderPort.send(any())).thenThrow(new EmailDeliveryException(
                DeliveryFailureType.RETRYABLE,
                "smtp timeout",
                null));

        useCase.processTask("delivery-1");

        ArgumentCaptor<AlertDeliveryRecord> captor = ArgumentCaptor.forClass(AlertDeliveryRecord.class);
        verify(alertDeliveryRepository, atLeast(2)).save(captor.capture());
        AlertDeliveryRecord finalState = captor.getValue();
        assertEquals(AlertDeliveryStatus.FAILED, finalState.getStatus());
        assertEquals(3, finalState.getAttemptCount());
        verify(dlqPublisher).publishFailure(any(AlertDeliveryRecord.class), any(), any());
    }

    private AlertDeliveryRecord pending(String id, int attempts) {
        return AlertDeliveryRecord.builder()
                .id(id)
                .alertId("alert-1")
                .userId("dev-admin")
                .channel(NotificationChannel.EMAIL)
                .destination("dev-admin@example.com")
                .status(AlertDeliveryStatus.PENDING)
                .attemptCount(attempts)
                .nextAttemptAt(Instant.now().minusSeconds(1))
                .createdAt(Instant.now().minusSeconds(60))
                .updatedAt(Instant.now().minusSeconds(60))
                .build();
    }
}
