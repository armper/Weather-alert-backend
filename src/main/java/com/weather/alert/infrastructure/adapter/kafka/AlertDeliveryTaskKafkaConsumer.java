package com.weather.alert.infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.application.usecase.ProcessAlertDeliveryTaskUseCase;
import com.weather.alert.infrastructure.config.NotificationDeliveryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
@Slf4j
public class AlertDeliveryTaskKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessAlertDeliveryTaskUseCase processAlertDeliveryTaskUseCase;
    private final NotificationDeliveryProperties properties;

    @KafkaListener(
            topics = "${app.notification.delivery.tasks-topic:weather-alert-delivery-tasks}",
            groupId = "alert-delivery-worker")
    public void consumeTask(String message) {
        if (!properties.isWorkerEnabled()) {
            return;
        }
        try {
            AlertDeliveryTaskMessage taskMessage = objectMapper.readValue(message, AlertDeliveryTaskMessage.class);
            if (taskMessage.deliveryId() == null || taskMessage.deliveryId().isBlank()) {
                return;
            }
            processAlertDeliveryTaskUseCase.processTask(taskMessage.deliveryId());
        } catch (Exception ex) {
            log.error("Error consuming delivery task message", ex);
        }
    }
}
