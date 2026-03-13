package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaPointProperties {
    private String forecast;
    private String forecastHourly;
    private String forecastGridData;
    private String observationStations;
    private String county;
    private String forecastZone;
    private String fireWeatherZone;
    private String forecastOffice;
}
