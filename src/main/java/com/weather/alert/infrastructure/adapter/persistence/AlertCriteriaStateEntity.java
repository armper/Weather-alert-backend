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
@Table(name = "criteria_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertCriteriaStateEntity {

    @Id
    @Column(name = "criteria_id", nullable = false)
    private String criteriaId;

    @Column(name = "last_condition_met", nullable = false)
    private Boolean lastConditionMet;

    @Column(name = "last_event_signature", length = 512)
    private String lastEventSignature;

    @Column(name = "last_notified_at")
    private Instant lastNotifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
