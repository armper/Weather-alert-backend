package com.weather.alert.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Weather conditions reported for a single stop on a truck route.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Weather data for one waypoint on the route")
public class RouteWeatherResponse {

    @Schema(description = "1-based position of this waypoint in the route", example = "1")
    private int sequence;

    @Schema(description = "Optional stop name", example = "Pickup – Memphis, TN")
    private String label;

    @Schema(description = "Latitude of the waypoint", example = "35.1495")
    private double latitude;

    @Schema(description = "Longitude of the waypoint", example = "-90.0490")
    private double longitude;

    @Schema(description = "Latest observed conditions at this waypoint (null if unavailable)")
    private WeatherDataResponse currentConditions;

    @Schema(description = "Active NWS weather alerts affecting this waypoint")
    private List<WeatherDataResponse> activeAlerts;
}
