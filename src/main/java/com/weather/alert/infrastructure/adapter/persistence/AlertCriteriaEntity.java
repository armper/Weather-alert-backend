package com.weather.alert.infrastructure.adapter.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alert_criteria")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertCriteriaEntity {
    
    @Id
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "radius_km")
    private Double radiusKm;
    
    @Column(name = "event_type")
    private String eventType;
    
    @Column(name = "min_severity")
    private String minSeverity;
    
    @Column(name = "max_temperature")
    private Double maxTemperature;
    
    @Column(name = "min_temperature")
    private Double minTemperature;
    
    @Column(name = "max_wind_speed")
    private Double maxWindSpeed;
    
    @Column(name = "max_precipitation")
    private Double maxPrecipitation;
    
    @Column(name = "enabled")
    private Boolean enabled;
}
