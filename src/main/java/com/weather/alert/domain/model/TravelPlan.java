package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

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
    private Instant createdAt;
    private Instant updatedAt;
}
