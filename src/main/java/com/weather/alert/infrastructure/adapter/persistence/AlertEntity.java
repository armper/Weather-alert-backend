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

    @Column(name = "event_key", length = 512)
    private String eventKey;

    @Column(name = "reason", length = 2000)
    private String reason;
    
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

    @Column(name = "condition_source", length = 64)
    private String conditionSource;

    @Column(name = "condition_onset")
    private Instant conditionOnset;

    @Column(name = "condition_expires")
    private Instant conditionExpires;

    @Column(name = "condition_temperature_c")
    private Double conditionTemperatureC;

    @Column(name = "condition_precipitation_probability")
    private Double conditionPrecipitationProbability;

    @Column(name = "condition_precipitation_amount")
    private Double conditionPrecipitationAmount;
    
    @Column(name = "alert_time")
    private Instant alertTime;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "sent_at")
    private Instant sentAt;
    
    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;
}
