package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.AlertResponse;
import com.weather.alert.application.usecase.QueryAlertsUseCase;
import com.weather.alert.domain.model.Alert;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for querying alerts
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Query generated user alerts")
public class AlertQueryController {
    
    private final QueryAlertsUseCase queryAlertsUseCase;
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get alerts by user ID")
    public ResponseEntity<List<AlertResponse>> getAlertsByUserId(
            @Parameter(example = "user-123") @PathVariable String userId) {
        List<Alert> alerts = queryAlertsUseCase.getAlertsByUserId(userId);
        List<AlertResponse> response = alerts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{alertId}")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<AlertResponse> getAlertById(
            @Parameter(example = "a8f1ee4d-5fd0-4b6a-a8ec-7cc7f4bced27") @PathVariable String alertId) {
        Alert alert = queryAlertsUseCase.getAlertById(alertId);
        return ResponseEntity.ok(toResponse(alert));
    }

    @GetMapping("/criteria/{criteriaId}/history")
    @Operation(summary = "Get alert history by criteria ID")
    public ResponseEntity<List<AlertResponse>> getAlertHistoryByCriteriaId(
            @Parameter(example = "ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6") @PathVariable String criteriaId) {
        List<Alert> alerts = queryAlertsUseCase.getAlertHistoryByCriteriaId(criteriaId);
        List<AlertResponse> response = alerts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pending")
    @Operation(summary = "Get pending alerts (admin)")
    public ResponseEntity<List<AlertResponse>> getPendingAlerts() {
        List<Alert> alerts = queryAlertsUseCase.getPendingAlerts();
        List<AlertResponse> response = alerts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge an alert")
    public ResponseEntity<AlertResponse> acknowledgeAlert(
            @Parameter(example = "a8f1ee4d-5fd0-4b6a-a8ec-7cc7f4bced27") @PathVariable String alertId) {
        Alert alert = queryAlertsUseCase.acknowledgeAlert(alertId);
        return ResponseEntity.ok(toResponse(alert));
    }

    @PostMapping("/{alertId}/expire")
    @Operation(summary = "Expire an alert")
    public ResponseEntity<AlertResponse> expireAlert(
            @Parameter(example = "a8f1ee4d-5fd0-4b6a-a8ec-7cc7f4bced27") @PathVariable String alertId) {
        Alert alert = queryAlertsUseCase.expireAlert(alertId);
        return ResponseEntity.ok(toResponse(alert));
    }
    
    private AlertResponse toResponse(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .userId(alert.getUserId())
                .criteriaId(alert.getCriteriaId())
                .eventKey(alert.getEventKey())
                .reason(alert.getReason())
                .eventType(alert.getEventType())
                .severity(alert.getSeverity())
                .headline(alert.getHeadline())
                .description(alert.getDescription())
                .location(alert.getLocation())
                .conditionSource(alert.getConditionSource())
                .conditionOnset(alert.getConditionOnset())
                .conditionExpires(alert.getConditionExpires())
                .conditionTemperatureC(alert.getConditionTemperatureC())
                .conditionPrecipitationProbability(alert.getConditionPrecipitationProbability())
                .conditionPrecipitationAmount(alert.getConditionPrecipitationAmount())
                .alertTime(alert.getAlertTime())
                .status(alert.getStatus() != null ? alert.getStatus().name() : null)
                .sentAt(alert.getSentAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .expiredAt(alert.getExpiredAt())
                .build();
    }
}
