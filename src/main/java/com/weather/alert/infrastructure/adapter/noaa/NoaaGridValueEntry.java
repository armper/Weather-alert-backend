package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaGridValueEntry {
    private String validTime;
    private Double value;
}
