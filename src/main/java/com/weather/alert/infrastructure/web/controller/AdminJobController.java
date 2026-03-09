package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.JobRunResponse;
import com.weather.alert.application.usecase.PublishDueAlertDeliveryTasksUseCase;
import com.weather.alert.application.usecase.RunDataRetentionCleanupUseCase;
import com.weather.alert.application.usecase.RunWeatherAlertProcessingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/jobs")
@RequiredArgsConstructor
@Tag(name = "Admin Jobs", description = "Admin-triggered operational jobs for Cloud Scheduler or manual runs")
public class AdminJobController {

    private final RunWeatherAlertProcessingUseCase runWeatherAlertProcessingUseCase;
    private final PublishDueAlertDeliveryTasksUseCase publishDueAlertDeliveryTasksUseCase;
    private final RunDataRetentionCleanupUseCase runDataRetentionCleanupUseCase;

    @PostMapping("/weather-processing")
    @Operation(
            summary = "Run weather alert processing",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job completed"),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Forbidden",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<JobRunResponse> runWeatherProcessing() {
        return ResponseEntity.ok(runWeatherAlertProcessingUseCase.run());
    }

    @PostMapping("/alert-delivery-retries")
    @Operation(
            summary = "Publish due alert delivery retries",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job completed"),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Forbidden",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<JobRunResponse> runAlertDeliveryRetries() {
        return ResponseEntity.ok(publishDueAlertDeliveryTasksUseCase.run());
    }

    @PostMapping("/data-retention")
    @Operation(
            summary = "Run data retention cleanup",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job completed"),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Forbidden",
                            content = @Content(mediaType = "application/problem+json"))
            })
    public ResponseEntity<JobRunResponse> runDataRetentionCleanup() {
        return ResponseEntity.ok(runDataRetentionCleanupUseCase.run());
    }
}
