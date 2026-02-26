package com.weather.alert.application.dto;

import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.UserNotificationPreference;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@Schema(description = "User-level notification preference")
public class UserNotificationPreferenceResponse {

    @Schema(example = "dev-admin")
    private String userId;

    @Schema(example = "[\"EMAIL\", \"SMS\"]")
    private List<NotificationChannel> enabledChannels;

    @Schema(example = "EMAIL")
    private NotificationChannel preferredChannel;

    @Schema(example = "FIRST_SUCCESS")
    private DeliveryFallbackStrategy fallbackStrategy;

    @Schema(example = "2026-02-26T17:00:00Z", nullable = true)
    private Instant createdAt;

    @Schema(example = "2026-02-26T17:10:00Z", nullable = true)
    private Instant updatedAt;

    public static UserNotificationPreferenceResponse fromDomain(UserNotificationPreference preference) {
        return UserNotificationPreferenceResponse.builder()
                .userId(preference.getUserId())
                .enabledChannels(preference.getEnabledChannels())
                .preferredChannel(preference.getPreferredChannel())
                .fallbackStrategy(preference.getFallbackStrategy())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}

