package com.weather.alert.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.weather.processing")
@Data
public class WeatherProcessingProperties {

    private boolean scheduleEnabled = true;
    private long fixedDelayMs = 300000;
    private long initialDelayMs = 30000;
}
