package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for alert response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Alert returned to a user")
public class AlertResponse {
    @Schema(example = "a8f1ee4d-5fd0-4b6a-a8ec-7cc7f4bced27")
    private String id;

    @Schema(example = "user-123")
    private String userId;

    @Schema(example = "Tornado Warning")
    private String eventType;

    @Schema(example = "SEVERE")
    private String severity;

    @Schema(example = "Tornado Warning for King County, WA")
    private String headline;

    @Schema(example = "The National Weather Service has issued a tornado warning until 5:30 PM PST.")
    private String description;

    @Schema(example = "King County, WA")
    private String location;

    @Schema(example = "2026-02-24T18:05:00Z")
    private Instant alertTime;

    @Schema(allowableValues = {"PENDING", "SENT", "ACKNOWLEDGED", "EXPIRED"}, example = "PENDING")
    private String status;
}
