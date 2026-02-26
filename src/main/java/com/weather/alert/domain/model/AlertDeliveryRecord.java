package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDeliveryRecord {
    private String id;
    private String alertId;
    private String userId;
    private NotificationChannel channel;
    private String destination;
    private AlertDeliveryStatus status;
    private Integer attemptCount;
    private String lastError;
    private String providerMessageId;
    private Instant sentAt;
    private Instant nextAttemptAt;
    private Instant createdAt;
    private Instant updatedAt;
}
