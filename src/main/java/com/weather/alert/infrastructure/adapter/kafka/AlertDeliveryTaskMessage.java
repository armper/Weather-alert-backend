package com.weather.alert.infrastructure.adapter.kafka;

import lombok.Builder;

import java.time.Instant;

@Builder
public record AlertDeliveryTaskMessage(
        String deliveryId,
        Instant requestedAt) {
}
