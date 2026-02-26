package com.weather.alert.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.notification.email")
@Data
public class NotificationEmailProperties {

    private String provider = "smtp";
    private String fromAddress = "no-reply@weather-alert.local";
    private Ses ses = new Ses();

    @Data
    public static class Ses {
        private String region = "us-east-1";
    }
}
