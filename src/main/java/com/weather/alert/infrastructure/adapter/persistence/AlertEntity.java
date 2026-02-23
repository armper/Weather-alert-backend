package com.weather.alert.infrastructure.adapter.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEntity {
    
    @Id
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "criteria_id")
    private String criteriaId;
    
    @Column(name = "weather_data_id")
    private String weatherDataId;
    
    @Column(name = "event_type")
    private String eventType;
    
    @Column(name = "severity")
    private String severity;
    
    @Column(name = "headline", length = 1000)
    private String headline;
    
    @Column(name = "description", length = 5000)
    private String description;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "alert_time")
    private Instant alertTime;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "sent_at")
    private Instant sentAt;
    
    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;
}
