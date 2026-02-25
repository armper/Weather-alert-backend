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

    @Schema(example = "ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6")
    private String criteriaId;

    @Schema(example = "forecast|ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6|2026-02-25T06:00:00Z")
    private String eventKey;

    @Schema(example = "Matched FORECAST: Chance Rain Showers")
    private String reason;

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

    @Schema(example = "FORECAST")
    private String conditionSource;

    @Schema(example = "2026-02-25T06:00:00Z")
    private Instant conditionOnset;

    @Schema(example = "2026-02-25T07:00:00Z")
    private Instant conditionExpires;

    @Schema(example = "13.4")
    private Double conditionTemperatureC;

    @Schema(example = "65")
    private Double conditionPrecipitationProbability;

    @Schema(example = "1.8")
    private Double conditionPrecipitationAmount;

    @Schema(example = "2026-02-24T18:05:00Z")
    private Instant alertTime;

    @Schema(allowableValues = {"PENDING", "SENT", "ACKNOWLEDGED", "EXPIRED"}, example = "PENDING")
    private String status;

    @Schema(example = "2026-02-24T18:05:03Z")
    private Instant sentAt;

    @Schema(example = "2026-02-24T18:07:00Z")
    private Instant acknowledgedAt;

    @Schema(example = "2026-02-24T19:00:00Z")
    private Instant expiredAt;
}
