package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaPointProperties {
    private String forecastHourly;
    private String observationStations;
}
