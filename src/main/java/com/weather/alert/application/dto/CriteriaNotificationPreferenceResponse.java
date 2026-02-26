package com.weather.alert.application.dto;

import com.weather.alert.domain.model.CriteriaNotificationPreference;
import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@Schema(description = "Criteria-level notification preference override")
public class CriteriaNotificationPreferenceResponse {

    @Schema(example = "ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6")
    private String criteriaId;

    @Schema(example = "true")
    private Boolean useUserDefaults;

    @Schema(example = "[\"EMAIL\"]", nullable = true)
    private List<NotificationChannel> enabledChannels;

    @Schema(example = "EMAIL", nullable = true)
    private NotificationChannel preferredChannel;

    @Schema(example = "FIRST_SUCCESS", nullable = true)
    private DeliveryFallbackStrategy fallbackStrategy;

    @Schema(example = "2026-02-26T17:00:00Z", nullable = true)
    private Instant createdAt;

    @Schema(example = "2026-02-26T17:10:00Z", nullable = true)
    private Instant updatedAt;

    public static CriteriaNotificationPreferenceResponse fromDomain(CriteriaNotificationPreference preference) {
        return CriteriaNotificationPreferenceResponse.builder()
                .criteriaId(preference.getCriteriaId())
                .useUserDefaults(preference.getUseUserDefaults())
                .enabledChannels(preference.getEnabledChannels())
                .preferredChannel(preference.getPreferredChannel())
                .fallbackStrategy(preference.getFallbackStrategy())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}

