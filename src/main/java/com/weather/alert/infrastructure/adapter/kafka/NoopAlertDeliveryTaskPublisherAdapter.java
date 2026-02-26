package com.weather.alert.infrastructure.adapter.kafka;

import com.weather.alert.domain.port.AlertDeliveryTaskPublisherPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(KafkaTemplate.class)
public class NoopAlertDeliveryTaskPublisherAdapter implements AlertDeliveryTaskPublisherPort {

    @Override
    public void publishTask(String deliveryId) {
        // no-op in environments without Kafka
    }
}
