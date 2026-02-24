package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaForecastPeriod {
    private String startTime;
    private String endTime;
    private Integer temperature;
    private String temperatureUnit;
    private String windSpeed;
    private String shortForecast;
    private String detailedForecast;
    private NoaaQuantitativeValue probabilityOfPrecipitation;
    private NoaaQuantitativeValue relativeHumidity;
}
