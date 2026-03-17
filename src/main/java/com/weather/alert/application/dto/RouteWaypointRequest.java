package com.weather.alert.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single stop on a truck route submitted by the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A waypoint on the route (a delivery stop, rest area, fuel stop, etc.)")
public class RouteWaypointRequest {

    @Min(value = 1, message = "sequence must be >= 1")
    @Schema(description = "1-based display order within the route", example = "1")
    private int sequence;

    @Size(max = 120, message = "label must be <= 120 characters")
    @Schema(description = "Optional human-readable stop name", example = "Pickup – Memphis, TN")
    private String label;

    @DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "latitude must be <= 90")
    @Schema(description = "Latitude of the waypoint", example = "35.1495")
    private double latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "longitude must be <= 180")
    @Schema(description = "Longitude of the waypoint", example = "-90.0490")
    private double longitude;
}
