package com.weather.alert.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.notification.delivery")
@Data
public class NotificationDeliveryProperties {

    private boolean workerEnabled = true;
    private String tasksTopic = "weather-alert-delivery-tasks";
    private String dlqTopic = "weather-alert-delivery-dlq";
    private int maxAttempts = 5;
    private long retryBaseSeconds = 30;
    private long retryMaxSeconds = 900;
    private long retryPollerFixedDelayMs = 10000;
    private long retryPollerInitialDelayMs = 15000;
    private int retryPollerBatchSize = 100;
}
