package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain model representing a weather alert sent to a user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    private String id;
    private String userId;
    private String criteriaId;
    private String weatherDataId;
    private String eventType;
    private String severity;
    private String headline;
    private String description;
    private String location;
    private Instant alertTime;
    private AlertStatus status;
    private Instant sentAt;
    private Instant acknowledgedAt;
    
    public enum AlertStatus {
        PENDING,
        SENT,
        ACKNOWLEDGED,
        EXPIRED
    }
}
