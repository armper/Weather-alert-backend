package com.weather.alert.domain.port;

import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.DeliveryFailureType;

public interface AlertDeliveryDlqPublisherPort {

    void publishFailure(AlertDeliveryRecord deliveryRecord, DeliveryFailureType failureType, String error);
}
