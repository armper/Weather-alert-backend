package com.weather.alert.infrastructure.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "channel_verifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelVerificationEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Column(name = "destination", nullable = false, length = 255)
    private String destination;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "verification_token_hash", length = 255)
    private String verificationTokenHash;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
