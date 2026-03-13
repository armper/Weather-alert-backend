package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

import java.util.List;

@Data
public class NoaaZoneForecastProperties {
    private String updated;
    private List<NoaaZoneForecastPeriod> periods;
}
