package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.JobRunResponse;
import com.weather.alert.domain.service.AlertProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunWeatherAlertProcessingUseCase {

    private final AlertProcessingService alertProcessingService;

    public JobRunResponse run() {
        Instant startedAt = Instant.now();
        log.info("Starting weather alert processing job");
        try {
            alertProcessingService.processWeatherAlerts();
            Instant finishedAt = Instant.now();
            long durationMillis = Duration.between(startedAt, finishedAt).toMillis();
            log.info("Weather alert processing job completed in {} ms", durationMillis);
            return JobRunResponse.builder()
                    .jobName("weather-processing")
                    .status("COMPLETED")
                    .startedAt(startedAt)
                    .finishedAt(finishedAt)
                    .durationMillis(durationMillis)
                    .metrics(Map.of())
                    .build();
        } catch (Exception ex) {
            log.error("Weather alert processing job failed", ex);
            throw ex;
        }
    }
}
