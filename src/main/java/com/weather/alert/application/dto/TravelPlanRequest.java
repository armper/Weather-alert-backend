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
                  "alertsEnabled": true
                }
                """)
public class TravelPlanRequest {

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

    @AssertTrue(message = "endDate must be on or after startDate")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "latitude and longitude must be provided together")
    public boolean isCoordinatePairValid() {
        return (latitude == null && longitude == null) || (latitude != null && longitude != null);
    }
}
