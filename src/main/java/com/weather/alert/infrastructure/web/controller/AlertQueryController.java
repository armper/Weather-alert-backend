package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.AlertResponse;
import com.weather.alert.application.dto.PagedResponse;
import com.weather.alert.application.exception.ForbiddenOperationException;
import com.weather.alert.application.usecase.QueryAlertsUseCase;
import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for querying alerts
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Validated
@Tag(name = "Alerts", description = "Query generated user alerts")
public class AlertQueryController {
    
    private final QueryAlertsUseCase queryAlertsUseCase;
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get alerts by user ID")
    public ResponseEntity<PagedResponse<AlertResponse>> getAlertsByUserId(
            @Parameter(example = "user-123") @PathVariable String userId,
            @Parameter(description = "Zero-based page index", example = "0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (max 200)", example = "50") @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            Authentication authentication) {
        enforceUserAccess(authentication, userId);
        PagedResult<Alert> alerts = queryAlertsUseCase.getAlertsByUserIdPaged(userId, page, size);
        return ResponseEntity.ok(toPagedResponse(alerts));
    }
    
    @GetMapping("/{alertId}")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<AlertResponse> getAlertById(
            @Parameter(example = "a8f1ee4d-5fd0-4b6a-a8ec-7cc7f4bced27") @PathVariable String alertId,
            Authentication authentication) {
        Alert alert = queryAlertsUseCase.getAlertById(alertId);
        enforceUserAccess(authentication, alert.getUserId());
        return ResponseEntity.ok(toResponse(alert));
    }

    @GetMapping("/criteria/{criteriaId}/history")
    @Operation(summary = "Get alert history by criteria ID")
    public ResponseEntity<PagedResponse<AlertResponse>> getAlertHistoryByCriteriaId(
            @Parameter(example = "ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6") @PathVariable String criteriaId,
            @Parameter(description = "Zero-based page index", example = "0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (max 200)", example = "50") @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            Authentication authentication) {
        AlertCriteria criteria = queryAlertsUseCase.getCriteriaById(criteriaId);
        enforceUserAccess(authentication, criteria.getUserId());
        PagedResult<Alert> alerts = queryAlertsUseCase.getAlertHistoryByCriteriaIdPaged(criteriaId, page, size);
        return ResponseEntity.ok(toPagedResponse(alerts));
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
            @Parameter(example = "a8f1ee4d-5fd0-4b6a-a8ec-7cc7f4bced27") @PathVariable String alertId,
            Authentication authentication) {
        Alert alert = queryAlertsUseCase.getAlertById(alertId);
        enforceUserAccess(authentication, alert.getUserId());
        Alert acknowledged = queryAlertsUseCase.acknowledgeAlert(alertId);
        return ResponseEntity.ok(toResponse(acknowledged));
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
                .conditionHumidity(alert.getConditionHumidity())
                .conditionDewPointC(alert.getConditionDewPointC())
                .conditionWindGust(alert.getConditionWindGust())
                .conditionSkyCover(alert.getConditionSkyCover())
                .conditionPrecipitationProbability(alert.getConditionPrecipitationProbability())
                .conditionPrecipitationAmount(alert.getConditionPrecipitationAmount())
                .conditionRiverGaugeId(alert.getConditionRiverGaugeId())
                .conditionRiverObservedStage(alert.getConditionRiverObservedStage())
                .conditionRiverForecastStage(alert.getConditionRiverForecastStage())
                .conditionRiverFloodStage(alert.getConditionRiverFloodStage())
                .conditionRiverActionStage(alert.getConditionRiverActionStage())
                .conditionRiverObservedCategory(alert.getConditionRiverObservedCategory())
                .conditionRiverForecastCategory(alert.getConditionRiverForecastCategory())
                .conditionRiverStageUnit(alert.getConditionRiverStageUnit())
                .alertTime(alert.getAlertTime())
                .status(alert.getStatus() != null ? alert.getStatus().name() : null)
                .sentAt(alert.getSentAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .expiredAt(alert.getExpiredAt())
                .build();
    }

    private PagedResponse<AlertResponse> toPagedResponse(PagedResult<Alert> paged) {
        return PagedResponse.<AlertResponse>builder()
                .items(paged.getItems().stream().map(this::toResponse).collect(Collectors.toList()))
                .page(paged.getPage())
                .size(paged.getSize())
                .totalElements(paged.getTotalElements())
                .totalPages(paged.getTotalPages())
                .hasNext(paged.isHasNext())
                .hasPrevious(paged.isHasPrevious())
                .build();
    }

    private void enforceUserAccess(Authentication authentication, String resourceOwnerUserId) {
        if (isAdmin(authentication)) {
            return;
        }
        String authenticatedUserId = authenticatedUserId(authentication);
        if (!authenticatedUserId.equals(resourceOwnerUserId)) {
            throw new ForbiddenOperationException("You do not have access to this alert");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String authenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ForbiddenOperationException("Unable to resolve authenticated user");
        }
        return authentication.getName();
    }
}
