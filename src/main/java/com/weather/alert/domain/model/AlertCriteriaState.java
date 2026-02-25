package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Persistent anti-spam state for alert criteria evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertCriteriaState {
    private String criteriaId;
    private Boolean lastConditionMet;
    private String lastEventSignature;
    private Instant lastNotifiedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
