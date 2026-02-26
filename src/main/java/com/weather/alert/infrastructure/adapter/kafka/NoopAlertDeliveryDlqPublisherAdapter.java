package com.weather.alert.infrastructure.adapter.kafka;

import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.port.AlertDeliveryDlqPublisherPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(KafkaTemplate.class)
public class NoopAlertDeliveryDlqPublisherAdapter implements AlertDeliveryDlqPublisherPort {

    @Override
    public void publishFailure(AlertDeliveryRecord deliveryRecord, DeliveryFailureType failureType, String error) {
        // no-op in environments without Kafka
    }
}
