package com.weather.alert.infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.domain.port.AlertDeliveryTaskPublisherPort;
import com.weather.alert.infrastructure.config.NotificationDeliveryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
@Slf4j
public class KafkaAlertDeliveryTaskPublisherAdapter implements AlertDeliveryTaskPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationDeliveryProperties properties;

    @Override
    public void publishTask(String deliveryId) {
        try {
            String message = objectMapper.writeValueAsString(AlertDeliveryTaskMessage.builder()
                    .deliveryId(deliveryId)
                    .requestedAt(Instant.now())
                    .build());
            kafkaTemplate.send(properties.getTasksTopic(), deliveryId, message);
        } catch (Exception ex) {
            log.error("Failed to publish delivery task {}", deliveryId, ex);
        }
    }
}
