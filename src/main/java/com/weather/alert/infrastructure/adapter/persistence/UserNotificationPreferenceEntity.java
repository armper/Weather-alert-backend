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
@Table(name = "user_notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationPreferenceEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "enabled_channels", nullable = false, length = 255)
    private String enabledChannels;

    @Column(name = "preferred_channel", nullable = false, length = 32)
    private String preferredChannel;

    @Column(name = "fallback_strategy", nullable = false, length = 32)
    private String fallbackStrategy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
