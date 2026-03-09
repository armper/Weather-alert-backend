package com.weather.alert.infrastructure.config;

import com.weather.alert.application.usecase.RunWeatherAlertProcessingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to periodically fetch weather data and process alerts
 */
@Component
@ConditionalOnProperty(
        value = "app.weather.processing.schedule-enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class WeatherAlertScheduler {
    
    private final RunWeatherAlertProcessingUseCase runWeatherAlertProcessingUseCase;
    
    /**
     * Fetch weather alerts every 5 minutes
     */
    @Scheduled(
            fixedDelayString = "${app.weather.processing.fixed-delay-ms:300000}",
            initialDelayString = "${app.weather.processing.initial-delay-ms:30000}")
    public void processWeatherAlerts() {
        runWeatherAlertProcessingUseCase.run();
    }
}
