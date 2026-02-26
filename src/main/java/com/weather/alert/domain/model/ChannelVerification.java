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
public class ChannelVerification {
    private String id;
    private String userId;
    private NotificationChannel channel;
    private String destination;
    private ChannelVerificationStatus status;
    private String verificationTokenHash;
    private Instant tokenExpiresAt;
    private Instant verifiedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
