package com.weather.alert.application.dto;

import com.weather.alert.domain.model.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to start channel verification")
public class StartChannelVerificationRequest {

    @NotNull
    @Schema(description = "Notification channel to verify", example = "EMAIL")
    private NotificationChannel channel;

    @NotBlank
    @Email
    @Schema(description = "Destination for the channel (email address for EMAIL)", example = "dev-admin@example.com")
    private String destination;
}
