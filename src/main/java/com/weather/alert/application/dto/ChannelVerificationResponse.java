package com.weather.alert.application.dto;

import com.weather.alert.domain.model.ChannelVerification;
import com.weather.alert.domain.model.ChannelVerificationStatus;
import com.weather.alert.domain.model.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@Schema(description = "Channel verification state")
public class ChannelVerificationResponse {

    @Schema(example = "2b4f4f31-5a4c-45d8-b274-301f8c6fb5f4")
    private String id;

    @Schema(example = "EMAIL")
    private NotificationChannel channel;

    @Schema(example = "dev-admin@example.com")
    private String destination;

    @Schema(example = "PENDING_VERIFICATION")
    private ChannelVerificationStatus status;

    @Schema(example = "2026-02-26T18:42:12Z", nullable = true)
    private Instant tokenExpiresAt;

    @Schema(example = "2026-02-26T18:40:00Z", nullable = true)
    private Instant verifiedAt;

    @Schema(
            description = "Returned only in local/dev when app.notification.verification.expose-raw-token=true",
            example = "2aQWQCi4k9c43-SprCuhbkJYE1S8rFf5",
            nullable = true)
    private String verificationToken;

    public static ChannelVerificationResponse fromDomain(ChannelVerification verification, String rawToken) {
        return ChannelVerificationResponse.builder()
                .id(verification.getId())
                .channel(verification.getChannel())
                .destination(verification.getDestination())
                .status(verification.getStatus())
                .tokenExpiresAt(verification.getTokenExpiresAt())
                .verifiedAt(verification.getVerifiedAt())
                .verificationToken(rawToken)
                .build();
    }
}
