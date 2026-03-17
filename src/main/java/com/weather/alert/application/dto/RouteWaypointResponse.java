package com.weather.alert.application.dto;

import com.weather.alert.domain.model.RouteWaypoint;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single stop on a truck route returned by the API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A waypoint on the route")
public class RouteWaypointResponse {

    @Schema(description = "1-based display order within the route", example = "1")
    private int sequence;

    @Schema(description = "Optional human-readable stop name", example = "Pickup – Memphis, TN")
    private String label;

    @Schema(description = "Latitude of the waypoint", example = "35.1495")
    private double latitude;

    @Schema(description = "Longitude of the waypoint", example = "-90.0490")
    private double longitude;

    public static RouteWaypointResponse fromDomain(RouteWaypoint waypoint) {
        return RouteWaypointResponse.builder()
                .sequence(waypoint.getSequence())
                .label(waypoint.getLabel())
                .latitude(waypoint.getLatitude())
                .longitude(waypoint.getLongitude())
                .build();
    }
}
