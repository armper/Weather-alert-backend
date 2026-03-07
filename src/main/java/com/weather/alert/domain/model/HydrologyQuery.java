package com.weather.alert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query for resolving NWPS hydrology conditions either by explicit gauge or by nearby coordinates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HydrologyQuery {
    private String gaugeId;
    private Double latitude;
    private Double longitude;
    private Double searchRadiusKm;
}
