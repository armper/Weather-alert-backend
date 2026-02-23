package com.weather.alert.infrastructure.adapter.noaa;

import lombok.Data;

@Data
public class NoaaAlertProperties {
    private String event;
    private String severity;
    private String certainty;
    private String urgency;
    private String headline;
    private String description;
    private String areaDesc;
    private String onset;
    private String expires;
    private String status;
    private String messageType;
    private String category;
}
