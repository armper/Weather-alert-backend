package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaObservationFeature {
    private String id;
    private NoaaObservationProperties properties;
}
