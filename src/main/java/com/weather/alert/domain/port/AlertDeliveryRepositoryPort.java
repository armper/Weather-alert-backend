package com.weather.alert.domain.port;

import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.NotificationChannel;

import java.util.Optional;

public interface AlertDeliveryRepositoryPort {

    AlertDeliveryRecord save(AlertDeliveryRecord deliveryRecord);

    Optional<AlertDeliveryRecord> findByAlertIdAndChannel(String alertId, NotificationChannel channel);
}
