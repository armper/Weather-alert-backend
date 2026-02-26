package com.weather.alert.infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.application.usecase.EnqueueAlertDeliveryUseCase;
import com.weather.alert.domain.model.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for processing alerts
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertKafkaConsumer {
    
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final EnqueueAlertDeliveryUseCase enqueueAlertDeliveryUseCase;
    
    @KafkaListener(topics = "weather-alerts", groupId = "alert-processor")
    public void consumeAlert(String message) {
        try {
            Alert alert = objectMapper.readValue(message, Alert.class);
            log.info("Consumed alert {} for user {}", alert.getId(), alert.getUserId());
            
            // Create async delivery tasks (email/SMS/push workers handle execution + retries)
            processAlert(alert);
            if (alert.getUserId() != null && !alert.getUserId().isBlank()) {
                simpMessagingTemplate.convertAndSend("/topic/alerts/" + alert.getUserId(), alert);
            }
        } catch (Exception e) {
            log.error("Error consuming alert message", e);
        }
    }
    
    private void processAlert(Alert alert) {
        enqueueAlertDeliveryUseCase.enqueue(alert);
    }
}
