package com.weather.alert.infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.EmailSendResult;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.AlertRepositoryPort;
import com.weather.alert.domain.port.EmailSenderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;

class AlertKafkaConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SimpMessagingTemplate simpMessagingTemplate = mock(SimpMessagingTemplate.class);
    private final AlertRepositoryPort alertRepository = mock(AlertRepositoryPort.class);
    private final UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
    private final EmailSenderPort emailSenderPort = mock(EmailSenderPort.class);
    private final AlertKafkaConsumer alertKafkaConsumer = new AlertKafkaConsumer(
            objectMapper,
            simpMessagingTemplate,
            alertRepository,
            userRepository,
            emailSenderPort);

    @Test
    void shouldBroadcastAlertToWebSocketTopic() throws Exception {
        Alert alert = Alert.builder()
                .id("alert-1")
                .userId("user-1")
                .headline("Headline")
                .description("Description")
                .status(Alert.AlertStatus.PENDING)
                .build();
        when(alertRepository.markAsSent(org.mockito.ArgumentMatchers.eq("alert-1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
        when(userRepository.findById("user-1")).thenReturn(Optional.of(User.builder()
                .id("user-1")
                .email("user-1@example.com")
                .build()));
        when(emailSenderPort.send(org.mockito.ArgumentMatchers.any(EmailMessage.class)))
                .thenReturn(new EmailSendResult("provider-message-1"));

        alertKafkaConsumer.consumeAlert(objectMapper.writeValueAsString(alert));

        verify(alertRepository).markAsSent(org.mockito.ArgumentMatchers.eq("alert-1"), org.mockito.ArgumentMatchers.any());
        verify(emailSenderPort).send(org.mockito.ArgumentMatchers.any(EmailMessage.class));
        verify(simpMessagingTemplate).convertAndSend("/topic/alerts/user-1", alert);
    }

    @Test
    void shouldNotBroadcastWhenPayloadIsInvalid() {
        alertKafkaConsumer.consumeAlert("{invalid-json");

        verifyNoInteractions(alertRepository);
        verifyNoInteractions(emailSenderPort);
        verifyNoInteractions(simpMessagingTemplate);
    }

    @Test
    void shouldNotBroadcastWhenUserIdIsMissing() throws Exception {
        Alert alert = Alert.builder()
                .id("alert-2")
                .headline("Headline")
                .description("Description")
                .status(Alert.AlertStatus.PENDING)
                .build();
        when(alertRepository.markAsSent(org.mockito.ArgumentMatchers.eq("alert-2"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        alertKafkaConsumer.consumeAlert(objectMapper.writeValueAsString(alert));

        verify(alertRepository).markAsSent(org.mockito.ArgumentMatchers.eq("alert-2"), org.mockito.ArgumentMatchers.any());
        verify(emailSenderPort, never()).send(org.mockito.ArgumentMatchers.any(EmailMessage.class));
        verifyNoInteractions(simpMessagingTemplate);
    }
}
