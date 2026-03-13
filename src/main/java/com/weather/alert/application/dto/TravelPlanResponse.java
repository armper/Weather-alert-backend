package com.weather.alert.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.weather.alert.domain.model.TravelPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Travel plan response payload")
public class TravelPlanResponse {

    private String id;
    private String userId;
    private String name;
    private String destination;
    private Double latitude;
    private Double longitude;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
    private Boolean alertsEnabled;
    private Instant createdAt;
    private Instant updatedAt;

    public static TravelPlanResponse fromDomain(TravelPlan plan) {
        return TravelPlanResponse.builder()
                .id(plan.getId())
                .userId(plan.getUserId())
                .name(plan.getName())
                .destination(plan.getDestination())
                .latitude(plan.getLatitude())
                .longitude(plan.getLongitude())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .notes(plan.getNotes())
                .alertsEnabled(plan.getAlertsEnabled())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    public static List<TravelPlanResponse> fromDomainList(List<TravelPlan> plans) {
        return plans.stream().map(TravelPlanResponse::fromDomain).collect(Collectors.toList());
    }
}
