package com.weather.alert.domain.port;

import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.NotificationChannel;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertDeliveryRepositoryPort {

    AlertDeliveryRecord save(AlertDeliveryRecord deliveryRecord);

    Optional<AlertDeliveryRecord> findById(String id);

    Optional<AlertDeliveryRecord> findByAlertIdAndChannel(String alertId, NotificationChannel channel);

    List<AlertDeliveryRecord> findDueForDelivery(Instant now, int limit);
}
