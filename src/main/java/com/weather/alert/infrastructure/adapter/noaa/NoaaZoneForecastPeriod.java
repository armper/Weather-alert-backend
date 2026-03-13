package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaZoneForecastPeriod {
    private String name;
    private String detailedForecast;
}
