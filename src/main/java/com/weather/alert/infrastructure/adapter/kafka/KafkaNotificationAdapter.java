package com.weather.alert.infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka adapter for publishing alerts
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationAdapter implements NotificationPort {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String ALERT_TOPIC = "weather-alerts";
    
    @Override
    public void sendAlert(Alert alert, String userId) {
        log.info("Sending alert {} to user {}", alert.getId(), userId);
        // This would integrate with actual notification services (email, SMS, push)
        // For now, we'll just publish to Kafka
        publishAlert(alert);
    }
    
    @Override
    public void publishAlert(Alert alert) {
        try {
            String message = objectMapper.writeValueAsString(alert);
            kafkaTemplate.send(ALERT_TOPIC, alert.getUserId(), message);
            log.info("Published alert {} to Kafka topic {}", alert.getId(), ALERT_TOPIC);
        } catch (Exception e) {
            log.error("Error publishing alert to Kafka", e);
        }
    }
}
