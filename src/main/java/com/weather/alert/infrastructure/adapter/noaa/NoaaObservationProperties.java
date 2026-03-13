package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaObservationProperties {
    private String timestamp;
    private String textDescription;
    private NoaaQuantitativeValue temperature;
    private NoaaQuantitativeValue windSpeed;
    private NoaaQuantitativeValue relativeHumidity;
    private NoaaQuantitativeValue precipitationLastHour;
    private NoaaQuantitativeValue windDirection;
    private NoaaQuantitativeValue visibility;
    private NoaaQuantitativeValue dewpoint;
    private NoaaQuantitativeValue windChill;
    private NoaaQuantitativeValue heatIndex;
}
