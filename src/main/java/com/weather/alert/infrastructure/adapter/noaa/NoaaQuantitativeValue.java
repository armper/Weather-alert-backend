package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaQuantitativeValue {
    private String unitCode;
    private Double value;
}
