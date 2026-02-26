package com.weather.alert.application.dto;

import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(
        description = "Update user-level notification preferences",
        example = """
                {
                  "enabledChannels": ["EMAIL", "SMS"],
                  "preferredChannel": "EMAIL",
                  "fallbackStrategy": "FIRST_SUCCESS"
                }
                """)
public class UpdateUserNotificationPreferenceRequest {

    @NotEmpty
    @Schema(description = "Enabled delivery channels in priority-capable order")
    private List<@NotNull NotificationChannel> enabledChannels;

    @NotNull
    @Schema(description = "Preferred first channel", example = "EMAIL")
    private NotificationChannel preferredChannel;

    @Schema(description = "Channel fallback strategy", example = "FIRST_SUCCESS")
    private DeliveryFallbackStrategy fallbackStrategy;

    @AssertTrue(message = "preferredChannel must be present in enabledChannels")
    public boolean isPreferredChannelIncluded() {
        return enabledChannels == null || preferredChannel == null || enabledChannels.contains(preferredChannel);
    }
}

