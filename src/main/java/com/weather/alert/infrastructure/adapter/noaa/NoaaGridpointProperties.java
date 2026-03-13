package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaGridpointProperties {
    private NoaaGridValueSeries relativeHumidity;
    private NoaaGridValueSeries dewpoint;
    private NoaaGridValueSeries windSpeed;
    private NoaaGridValueSeries windGust;
    private NoaaGridValueSeries skyCover;
    private NoaaGridValueSeries probabilityOfPrecipitation;
    private NoaaGridValueSeries quantitativePrecipitation;
    private NoaaGridValueSeries apparentTemperature;
    private NoaaGridValueSeries windChill;
    private NoaaGridValueSeries heatIndex;
    private NoaaGridValueSeries visibility;
    private NoaaGridValueSeries windDirection;
    private NoaaGridValueSeries snowfallAmount;
    private NoaaGridValueSeries iceAccumulation;
    private NoaaGridValueSeries probabilityOfThunder;
    private NoaaGridValueSeries ceilingHeight;
}
