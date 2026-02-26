package com.weather.alert.application.dto;

import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.util.List;

@Data
@Schema(
        description = "Update criteria-level notification preference override",
        example = """
                {
                  "useUserDefaults": false,
                  "enabledChannels": ["EMAIL"],
                  "preferredChannel": "EMAIL",
                  "fallbackStrategy": "FIRST_SUCCESS"
                }
                """)
public class UpdateCriteriaNotificationPreferenceRequest {

    @Schema(
            description = "When true, criteria uses user-level defaults and ignores explicit override fields",
            example = "false")
    private Boolean useUserDefaults;

    @Schema(description = "Enabled channels when useUserDefaults=false")
    private List<NotificationChannel> enabledChannels;

    @Schema(description = "Preferred channel when useUserDefaults=false", example = "EMAIL")
    private NotificationChannel preferredChannel;

    @Schema(description = "Fallback strategy when useUserDefaults=false", example = "FIRST_SUCCESS")
    private DeliveryFallbackStrategy fallbackStrategy;

    @AssertTrue(message = "enabledChannels must be provided when useUserDefaults is false")
    public boolean isEnabledChannelsProvidedWhenNotUsingDefaults() {
        if (Boolean.TRUE.equals(useUserDefaults) || useUserDefaults == null) {
            return true;
        }
        return enabledChannels != null && !enabledChannels.isEmpty();
    }

    @AssertTrue(message = "preferredChannel must be present in enabledChannels when useUserDefaults is false")
    public boolean isPreferredChannelIncludedWhenNotUsingDefaults() {
        if (Boolean.TRUE.equals(useUserDefaults) || useUserDefaults == null) {
            return true;
        }
        if (preferredChannel == null || enabledChannels == null || enabledChannels.isEmpty()) {
            return false;
        }
        return enabledChannels.contains(preferredChannel);
    }

    @AssertTrue(message = "enabledChannels, preferredChannel, and fallbackStrategy must be omitted when useUserDefaults is true")
    public boolean isOverridePayloadEmptyWhenUsingDefaults() {
        if (!Boolean.TRUE.equals(useUserDefaults) && useUserDefaults != null) {
            return true;
        }
        boolean hasChannels = enabledChannels != null && !enabledChannels.isEmpty();
        return !hasChannels && preferredChannel == null && fallbackStrategy == null;
    }
}

