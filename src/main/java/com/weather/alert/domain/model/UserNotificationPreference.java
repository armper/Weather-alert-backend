package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationPreference {
    private String userId;
    private List<NotificationChannel> enabledChannels;
    private NotificationChannel preferredChannel;
    private DeliveryFallbackStrategy fallbackStrategy;
    private Instant createdAt;
    private Instant updatedAt;
}
