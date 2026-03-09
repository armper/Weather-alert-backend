package com.weather.alert.infrastructure.adapter.notification;

import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.port.AlertDeliveryDlqPublisherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingAlertDeliveryDlqPublisherAdapter implements AlertDeliveryDlqPublisherPort {

    @Override
    public void publishFailure(AlertDeliveryRecord deliveryRecord, DeliveryFailureType failureType, String error) {
        if (deliveryRecord == null) {
            return;
        }
        log.error(
                "Delivery moved to terminal failure state deliveryId={} alertId={} userId={} channel={} failureType={} error={}",
                deliveryRecord.getId(),
                deliveryRecord.getAlertId(),
                deliveryRecord.getUserId(),
                deliveryRecord.getChannel(),
                failureType,
                error);
    }
}
