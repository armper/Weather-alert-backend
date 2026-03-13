package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

import java.util.List;

@Data
public class NoaaObservationListResponse {
    private List<NoaaObservationFeature> features;
}
