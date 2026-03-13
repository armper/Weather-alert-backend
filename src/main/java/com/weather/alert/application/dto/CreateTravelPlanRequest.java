package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for creating or updating a travel plan")
public class CreateTravelPlanRequest {

    @Schema(description = "User identifier (optional for non-admin; inferred from JWT)", example = "user-123")
    private String userId;

    @NotBlank(message = "name is required")
    @Schema(description = "User-defined trip name", example = "NYC Business Trip")
    private String name;

    @NotBlank(message = "destination is required")
    @Schema(description = "Destination city or place name", example = "New York City")
    private String destination;

    @Schema(example = "40.7128")
    private Double latitude;

    @Schema(example = "-74.0060")
    private Double longitude;

    @NotNull(message = "startDate is required")
    @Schema(description = "Trip start date (inclusive, ISO-8601 date)", example = "2026-06-15")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    @Schema(description = "Trip end date (inclusive, ISO-8601 date)", example = "2026-06-20")
    private LocalDate endDate;

    @Schema(description = "Optional free-text notes", example = "Outdoor conference")
    private String notes;

    @Schema(description = "Whether to send weather alerts for this trip", example = "true")
    private Boolean alertsEnabled;
}
