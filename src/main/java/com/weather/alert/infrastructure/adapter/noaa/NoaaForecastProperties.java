package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

import java.util.List;

@Data
public class NoaaForecastProperties {
    private List<NoaaForecastPeriod> periods;
}
