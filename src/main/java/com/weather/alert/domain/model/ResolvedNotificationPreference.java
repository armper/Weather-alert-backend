package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedNotificationPreference {
    private String userId;
    private String criteriaId;
    private boolean criteriaOverrideApplied;
    private List<NotificationChannel> orderedChannels;
    private NotificationChannel preferredChannel;
    private DeliveryFallbackStrategy fallbackStrategy;
}
