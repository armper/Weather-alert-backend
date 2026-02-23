package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

import java.util.List;

@Data
public class NoaaAlertResponse {
    private List<NoaaAlertFeature> features;
}
