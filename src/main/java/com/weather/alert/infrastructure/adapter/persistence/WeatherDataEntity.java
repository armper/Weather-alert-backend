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
@Table(name = "weather_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDataEntity {

    @Id
    private String id;

    @Column(name = "location", columnDefinition = "TEXT")
    private String location;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "event_type", length = 255)
    private String eventType;

    @Column(name = "severity", length = 255)
    private String severity;

    @Column(name = "headline", length = 1000)
    private String headline;

    @Column(name = "description", length = 5000)
    private String description;

    @Column(name = "onset")
    private Instant onset;

    @Column(name = "expires")
    private Instant expires;

    @Column(name = "status", length = 255)
    private String status;

    @Column(name = "message_type", length = 255)
    private String messageType;

    @Column(name = "category", length = 255)
    private String category;

    @Column(name = "urgency", length = 255)
    private String urgency;

    @Column(name = "certainty", length = 255)
    private String certainty;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "precipitation")
    private Double precipitation;

    @Column(name = "precipitation_probability")
    private Double precipitationProbability;

    @Column(name = "precipitation_amount")
    private Double precipitationAmount;

    @Column(name = "humidity")
    private Double humidity;

    @Column(name = "dew_point")
    private Double dewPoint;

    @Column(name = "wind_gust")
    private Double windGust;

    @Column(name = "sky_cover")
    private Double skyCover;

    @Column(name = "river_gauge_id", length = 255)
    private String riverGaugeId;

    @Column(name = "river_observed_stage")
    private Double riverObservedStage;

    @Column(name = "river_forecast_stage")
    private Double riverForecastStage;

    @Column(name = "river_flood_stage")
    private Double riverFloodStage;

    @Column(name = "river_action_stage")
    private Double riverActionStage;

    @Column(name = "river_observed_category", length = 255)
    private String riverObservedCategory;

    @Column(name = "river_forecast_category", length = 255)
    private String riverForecastCategory;

    @Column(name = "river_stage_unit", length = 32)
    private String riverStageUnit;

    @Column(name = "river_distance_km")
    private Double riverDistanceKm;

    @Column(name = "recorded_at")
    private Instant recordedAt;
}
