package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaStationProperties {
    private String stationIdentifier;
    private String name;
}
