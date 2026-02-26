package com.weather.alert.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.weather.alert.domain.model.AlertCriteria;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Alert criteria response payload. Null fields are omitted for concise responses.")
public class AlertCriteriaResponse {

    private String id;
    private String name;
    private String userId;
    private String location;
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private String eventType;
    private String minSeverity;
    private Double maxTemperature;
    private Double minTemperature;
    private Double maxWindSpeed;
    private Double maxPrecipitation;
    private Double temperatureThreshold;
    private AlertCriteria.TemperatureDirection temperatureDirection;
    private Double rainThreshold;
    private AlertCriteria.RainThresholdType rainThresholdType;
    private Boolean monitorCurrent;
    private Boolean monitorForecast;
    private Integer forecastWindowHours;
    private AlertCriteria.TemperatureUnit temperatureUnit;
    private Boolean oncePerEvent;
    private Integer rearmWindowMinutes;
    private Boolean enabled;

    public static AlertCriteriaResponse fromDomain(AlertCriteria criteria) {
        if (criteria == null) {
            return null;
        }
        return AlertCriteriaResponse.builder()
                .id(criteria.getId())
                .name(criteria.getName())
                .userId(criteria.getUserId())
                .location(criteria.getLocation())
                .latitude(criteria.getLatitude())
                .longitude(criteria.getLongitude())
                .radiusKm(criteria.getRadiusKm())
                .eventType(criteria.getEventType())
                .minSeverity(criteria.getMinSeverity())
                .maxTemperature(criteria.getMaxTemperature())
                .minTemperature(criteria.getMinTemperature())
                .maxWindSpeed(criteria.getMaxWindSpeed())
                .maxPrecipitation(criteria.getMaxPrecipitation())
                .temperatureThreshold(criteria.getTemperatureThreshold())
                .temperatureDirection(criteria.getTemperatureDirection())
                .rainThreshold(criteria.getRainThreshold())
                .rainThresholdType(criteria.getRainThresholdType())
                .monitorCurrent(criteria.getMonitorCurrent())
                .monitorForecast(criteria.getMonitorForecast())
                .forecastWindowHours(criteria.getForecastWindowHours())
                .temperatureUnit(criteria.getTemperatureUnit())
                .oncePerEvent(criteria.getOncePerEvent())
                .rearmWindowMinutes(criteria.getRearmWindowMinutes())
                .enabled(criteria.getEnabled())
                .build();
    }

    public static List<AlertCriteriaResponse> fromDomainList(List<AlertCriteria> criteria) {
        return criteria.stream().map(AlertCriteriaResponse::fromDomain).toList();
    }
}
