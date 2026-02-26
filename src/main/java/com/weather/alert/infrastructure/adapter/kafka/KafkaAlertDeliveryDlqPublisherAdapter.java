package com.weather.alert.infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.port.AlertDeliveryDlqPublisherPort;
import com.weather.alert.infrastructure.config.NotificationDeliveryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaAlertDeliveryDlqPublisherAdapter implements AlertDeliveryDlqPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationDeliveryProperties properties;

    @Override
    public void publishFailure(AlertDeliveryRecord deliveryRecord, DeliveryFailureType failureType, String error) {
        if (deliveryRecord == null || deliveryRecord.getId() == null) {
            return;
        }
        try {
            String message = objectMapper.writeValueAsString(AlertDeliveryDlqMessage.builder()
                    .deliveryId(deliveryRecord.getId())
                    .alertId(deliveryRecord.getAlertId())
                    .userId(deliveryRecord.getUserId())
                    .channel(deliveryRecord.getChannel())
                    .destination(deliveryRecord.getDestination())
                    .attemptCount(deliveryRecord.getAttemptCount())
                    .failureType(failureType)
                    .error(error)
                    .occurredAt(Instant.now())
                    .build());
            kafkaTemplate.send(properties.getDlqTopic(), deliveryRecord.getId(), message);
        } catch (Exception ex) {
            log.error("Failed to publish delivery failure to DLQ for delivery {}", deliveryRecord.getId(), ex);
        }
    }
}
