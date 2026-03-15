package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaDailyForecastPeriod {
    private Integer number;
    private String name;
    private String startTime;
    private String endTime;
    private Boolean isDaytime;
    private Integer temperature;
    private String temperatureUnit;
    private String windSpeed;
    private String windDirection;
    private String shortForecast;
    private String detailedForecast;
    private String icon;
    private NoaaQuantitativeValue probabilityOfPrecipitation;
}
