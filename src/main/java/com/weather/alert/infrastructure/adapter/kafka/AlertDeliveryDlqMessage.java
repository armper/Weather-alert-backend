package com.weather.alert.infrastructure.adapter.kafka;

import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.model.NotificationChannel;
import lombok.Builder;

import java.time.Instant;

@Builder
public record AlertDeliveryDlqMessage(
        String deliveryId,
        String alertId,
        String userId,
        NotificationChannel channel,
        String destination,
        Integer attemptCount,
        DeliveryFailureType failureType,
        String error,
        Instant occurredAt) {
}
