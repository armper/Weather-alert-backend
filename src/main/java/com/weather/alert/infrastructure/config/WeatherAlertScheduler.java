package com.weather.alert.infrastructure.config;

import com.weather.alert.domain.service.AlertProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

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
    @Scheduled(fixedDelay = 300000, initialDelay = 30000) // every 5 minutes, wait for previous run to finish
    public void processWeatherAlerts() {
        Instant start = Instant.now();
        log.info("Starting scheduled weather alert processing");
        try {
            alertProcessingService.processWeatherAlerts();
            log.info("Scheduled weather alert processing completed in {} ms", Duration.between(start, Instant.now()).toMillis());
        } catch (Exception e) {
            log.error("Error processing weather alerts", e);
        }
    }
}
