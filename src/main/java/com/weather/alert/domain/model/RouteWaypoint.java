package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single stop on a truck driver's route, used for per-waypoint weather monitoring.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteWaypoint {

    /** 1-based display order within the route. */
    private int sequence;

    /** Optional human-readable label (e.g. "Pickup – Memphis, TN"). */
    private String label;

    private double latitude;
    private double longitude;
}
