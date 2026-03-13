package com.weather.alert.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Domain model representing a user's travel plan with associated weather monitoring.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A user travel plan with optional weather alert monitoring")
public class TravelPlan {

    @Schema(example = "tp-abc123")
    private String id;

    @Schema(example = "user-123")
    private String userId;

    @Schema(description = "User-defined trip name", example = "NYC Business Trip")
    private String name;

    @Schema(description = "Destination city or place name", example = "New York City")
    private String destination;

    @Schema(example = "40.7128")
    private Double latitude;

    @Schema(example = "-74.0060")
    private Double longitude;

    @Schema(description = "Trip start date (inclusive)", example = "2026-06-15")
    private LocalDate startDate;

    @Schema(description = "Trip end date (inclusive)", example = "2026-06-20")
    private LocalDate endDate;

    @Schema(description = "Optional free-text notes", example = "Outdoor conference, need weather updates")
    private String notes;

    @Schema(description = "Whether weather alerts are enabled for this trip", example = "true")
    private Boolean alertsEnabled;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Instant createdAt;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Instant updatedAt;
}
