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
@Table(name = "alert_delivery")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDeliveryEntity {

    @Id
    private String id;

    @Column(name = "alert_id", nullable = false)
    private String alertId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Column(name = "destination", nullable = false, length = 255)
    private String destination;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
