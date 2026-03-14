package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        description = "Create or update a saved travel plan",
        example = """
                {
                  "userId": "weather-admin",
                  "name": "NYC Conference",
                  "destination": "New York City",
                  "startDate": "2026-05-18",
                  "endDate": "2026-05-22",
                  "latitude": 40.7128,
                  "longitude": -74.0060,
                  "notes": "Pack an umbrella for afternoon events.",
                  "alertsEnabled": true,
                  "alertCoverageMode": "TOPICS",
                  "selectedAlertTopics": ["RAIN", "WIND"],
                  "linkedCriteriaIds": []
                }
                """)
public class TravelPlanRequest {

    private static final Set<String> ALLOWED_COVERAGE_MODES = Set.of("ALL_ALERTS", "TOPICS", "LINKED_RULES");
    private static final Set<String> ALLOWED_ALERT_TOPICS = Set.of("RAIN", "WIND", "HEAT", "COLD", "HUMIDITY", "SKY", "RIVER");

    @Schema(description = "User identifier that owns this travel plan (optional for non-admin; inferred from JWT subject)", example = "weather-admin")
    private String userId;

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must be <= 120 characters")
    @Schema(description = "Short trip label", example = "NYC Conference")
    private String name;

    @NotBlank(message = "destination is required")
    @Size(max = 180, message = "destination must be <= 180 characters")
    @Schema(description = "Destination name shown in the UI", example = "New York City")
    private String destination;

    @NotNull(message = "startDate is required")
    @Schema(description = "Trip start date in ISO-8601 format", example = "2026-05-18")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    @Schema(description = "Trip end date in ISO-8601 format", example = "2026-05-22")
    private LocalDate endDate;

    @DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "latitude must be <= 90")
    @Schema(description = "Optional destination latitude", example = "40.7128")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "longitude must be <= 180")
    @Schema(description = "Optional destination longitude", example = "-74.0060")
    private Double longitude;

    @Size(max = 500, message = "notes must be <= 500 characters")
    @Schema(description = "Optional travel notes", example = "Pack an umbrella for afternoon events.")
    private String notes;

    @Schema(description = "Whether trip weather alerts are enabled", example = "true")
    private Boolean alertsEnabled;

    @Schema(description = "How this trip decides which alerts matter", example = "TOPICS")
    private String alertCoverageMode;

    @Schema(description = "Weather topics watched for this trip when alertCoverageMode is TOPICS", example = "[\"RAIN\", \"WIND\"]")
    private List<String> selectedAlertTopics;

    @Schema(description = "Saved alert criteria ids linked to this trip when alertCoverageMode is LINKED_RULES", example = "[\"criteria-1\"]")
    private List<String> linkedCriteriaIds;

    @AssertTrue(message = "endDate must be on or after startDate")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "latitude and longitude must be provided together")
    public boolean isCoordinatePairValid() {
        return (latitude == null && longitude == null) || (latitude != null && longitude != null);
    }

    @AssertTrue(message = "alertCoverageMode must be ALL_ALERTS, TOPICS, or LINKED_RULES")
    public boolean isAlertCoverageModeValid() {
        return alertCoverageMode == null || alertCoverageMode.isBlank() || ALLOWED_COVERAGE_MODES.contains(alertCoverageMode.trim().toUpperCase());
    }

    @AssertTrue(message = "selectedAlertTopics contains an unsupported topic")
    public boolean areSelectedAlertTopicsValid() {
        if (selectedAlertTopics == null) {
            return true;
        }
        return selectedAlertTopics.stream()
                .filter(topic -> topic != null && !topic.isBlank())
                .map(topic -> topic.trim().toUpperCase())
                .allMatch(ALLOWED_ALERT_TOPICS::contains);
    }

    @AssertTrue(message = "linkedCriteriaIds contains a blank rule id")
    public boolean areLinkedCriteriaIdsValid() {
        if (linkedCriteriaIds == null) {
            return true;
        }
        return linkedCriteriaIds.stream().allMatch(criteriaId -> criteriaId != null && !criteriaId.isBlank());
    }
}
