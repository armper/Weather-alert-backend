package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.AlertCriteria;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "alert_criteria")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertCriteriaEntity {
    
    @Id
    private String id;

    @Column(name = "name", length = 120)
    private String name;
    
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

    @Column(name = "temperature_threshold")
    private Double temperatureThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "temperature_direction")
    private AlertCriteria.TemperatureDirection temperatureDirection;

    @Column(name = "rain_threshold")
    private Double rainThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "rain_threshold_type")
    private AlertCriteria.RainThresholdType rainThresholdType;

    @Column(name = "humidity_threshold")
    private Double humidityThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "humidity_direction")
    private AlertCriteria.ComparisonDirection humidityDirection;

    @Column(name = "dew_point_threshold")
    private Double dewPointThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "dew_point_direction")
    private AlertCriteria.ComparisonDirection dewPointDirection;

    @Column(name = "wind_gust_threshold")
    private Double windGustThreshold;

    @Column(name = "sky_cover_threshold")
    private Double skyCoverThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "sky_cover_direction")
    private AlertCriteria.ComparisonDirection skyCoverDirection;

    @Column(name = "river_gauge_id")
    private String riverGaugeId;

    @Column(name = "river_stage_threshold")
    private Double riverStageThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "river_stage_direction")
    private AlertCriteria.ComparisonDirection riverStageDirection;

    @Enumerated(EnumType.STRING)
    @Column(name = "river_flood_category_threshold")
    private AlertCriteria.FloodCategory riverFloodCategoryThreshold;

    @Column(name = "monitor_current")
    private Boolean monitorCurrent;

    @Column(name = "monitor_forecast")
    private Boolean monitorForecast;

    @Column(name = "forecast_window_hours")
    private Integer forecastWindowHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "temperature_unit")
    private AlertCriteria.TemperatureUnit temperatureUnit;

    @Column(name = "once_per_event")
    private Boolean oncePerEvent;

    @Column(name = "rearm_window_minutes")
    private Integer rearmWindowMinutes;
    
    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
