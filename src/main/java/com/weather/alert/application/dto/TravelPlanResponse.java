package com.weather.alert.application.dto;

import com.weather.alert.domain.model.TravelPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Saved travel plan")
public class TravelPlanResponse {

    private String id;
    private String userId;
    private String name;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double latitude;
    private Double longitude;
    private String notes;
    private Boolean alertsEnabled;
    private String alertCoverageMode;
    private List<String> selectedAlertTopics;
    private List<String> linkedCriteriaIds;
    private Instant createdAt;
    private Instant updatedAt;

    public static TravelPlanResponse fromDomain(TravelPlan travelPlan) {
        return TravelPlanResponse.builder()
                .id(travelPlan.getId())
                .userId(travelPlan.getUserId())
                .name(travelPlan.getName())
                .destination(travelPlan.getDestination())
                .startDate(travelPlan.getStartDate())
                .endDate(travelPlan.getEndDate())
                .latitude(travelPlan.getLatitude())
                .longitude(travelPlan.getLongitude())
                .notes(travelPlan.getNotes())
                .alertsEnabled(travelPlan.getAlertsEnabled())
                .alertCoverageMode(travelPlan.getAlertCoverageMode())
                .selectedAlertTopics(travelPlan.getSelectedAlertTopics())
                .linkedCriteriaIds(travelPlan.getLinkedCriteriaIds())
                .createdAt(travelPlan.getCreatedAt())
                .updatedAt(travelPlan.getUpdatedAt())
                .build();
    }

    public static List<TravelPlanResponse> fromDomainList(List<TravelPlan> travelPlans) {
        return travelPlans.stream().map(TravelPlanResponse::fromDomain).toList();
    }
}
