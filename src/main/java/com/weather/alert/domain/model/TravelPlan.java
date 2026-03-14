package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlan {

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
    private List<RouteWaypoint> waypoints;
    private Instant createdAt;
    private Instant updatedAt;
}
