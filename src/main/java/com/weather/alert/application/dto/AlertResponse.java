package com.weather.alert.application.dto;

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
public class AlertResponse {
    private String id;
    private String userId;
    private String eventType;
    private String severity;
    private String headline;
    private String description;
    private String location;
    private Instant alertTime;
    private String status;
}
