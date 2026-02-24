package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

import java.util.List;

@Data
public class NoaaStationsResponse {
    private List<NoaaStationFeature> features;
}
