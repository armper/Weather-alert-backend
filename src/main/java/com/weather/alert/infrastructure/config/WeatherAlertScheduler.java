package com.weather.alert.infrastructure.config;

import com.weather.alert.domain.service.AlertProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to periodically fetch weather data and process alerts
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherAlertScheduler {
    
    private final AlertProcessingService alertProcessingService;
    
    /**
     * Fetch weather alerts every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void processWeatherAlerts() {
        log.info("Starting scheduled weather alert processing");
        try {
            alertProcessingService.processWeatherAlerts();
        } catch (Exception e) {
            log.error("Error processing weather alerts", e);
        }
    }
}
