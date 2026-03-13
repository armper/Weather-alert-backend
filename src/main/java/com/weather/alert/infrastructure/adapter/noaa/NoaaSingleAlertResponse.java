package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaSingleAlertResponse {
    private String id;
    private NoaaAlertProperties properties;
}
