package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.AlertResponse;
import com.weather.alert.application.usecase.QueryAlertsUseCase;
import com.weather.alert.domain.model.Alert;
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
public class AlertQueryController {
    
    private final QueryAlertsUseCase queryAlertsUseCase;
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AlertResponse>> getAlertsByUserId(@PathVariable String userId) {
        List<Alert> alerts = queryAlertsUseCase.getAlertsByUserId(userId);
        List<AlertResponse> response = alerts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{alertId}")
    public ResponseEntity<AlertResponse> getAlertById(@PathVariable String alertId) {
        Alert alert = queryAlertsUseCase.getAlertById(alertId);
        return ResponseEntity.ok(toResponse(alert));
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<AlertResponse>> getPendingAlerts() {
        List<Alert> alerts = queryAlertsUseCase.getPendingAlerts();
        List<AlertResponse> response = alerts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    private AlertResponse toResponse(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .userId(alert.getUserId())
                .eventType(alert.getEventType())
                .severity(alert.getSeverity())
                .headline(alert.getHeadline())
                .description(alert.getDescription())
                .location(alert.getLocation())
                .alertTime(alert.getAlertTime())
                .status(alert.getStatus() != null ? alert.getStatus().name() : null)
                .build();
    }
}
