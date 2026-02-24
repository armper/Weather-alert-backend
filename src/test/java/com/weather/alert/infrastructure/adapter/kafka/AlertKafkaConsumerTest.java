package com.weather.alert.infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.domain.model.Alert;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AlertKafkaConsumerTest {

    private final SimpMessagingTemplate simpMessagingTemplate = mock(SimpMessagingTemplate.class);
    private final AlertKafkaConsumer alertKafkaConsumer = new AlertKafkaConsumer(new ObjectMapper(), simpMessagingTemplate);

    @Test
    void shouldBroadcastAlertToWebSocketTopic() throws Exception {
        Alert alert = Alert.builder()
                .id("alert-1")
                .userId("user-1")
                .headline("Headline")
                .description("Description")
                .status(Alert.AlertStatus.PENDING)
                .build();

        alertKafkaConsumer.consumeAlert(new ObjectMapper().writeValueAsString(alert));

        verify(simpMessagingTemplate).convertAndSend("/topic/alerts", alert);
    }

    @Test
    void shouldNotBroadcastWhenPayloadIsInvalid() {
        alertKafkaConsumer.consumeAlert("{invalid-json");

        verifyNoInteractions(simpMessagingTemplate);
    }
}
