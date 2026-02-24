package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
import com.weather.alert.application.usecase.ManageAlertCriteriaUseCase;
import com.weather.alert.application.usecase.QueryAlertsUseCase;
import com.weather.alert.domain.model.AlertCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing alert criteria
 */
@RestController
@RequestMapping("/api/criteria")
@RequiredArgsConstructor
@Tag(name = "Alert Criteria", description = "Create and manage user alert criteria")
public class AlertCriteriaController {
    
    private final ManageAlertCriteriaUseCase manageAlertCriteriaUseCase;
    private final QueryAlertsUseCase queryAlertsUseCase;
    
    @PostMapping
    @Operation(summary = "Create alert criteria")
    public ResponseEntity<AlertCriteria> createCriteria(@RequestBody CreateAlertCriteriaRequest request) {
        AlertCriteria criteria = manageAlertCriteriaUseCase.createCriteria(request);
        return ResponseEntity.ok(criteria);
    }
    
    @PutMapping("/{criteriaId}")
    @Operation(summary = "Update alert criteria")
    public ResponseEntity<AlertCriteria> updateCriteria(
            @Parameter(example = "ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6") @PathVariable String criteriaId,
            @RequestBody CreateAlertCriteriaRequest request) {
        AlertCriteria criteria = manageAlertCriteriaUseCase.updateCriteria(criteriaId, request);
        return ResponseEntity.ok(criteria);
    }
    
    @DeleteMapping("/{criteriaId}")
    @Operation(summary = "Delete alert criteria")
    public ResponseEntity<Void> deleteCriteria(
            @Parameter(example = "ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6") @PathVariable String criteriaId) {
        manageAlertCriteriaUseCase.deleteCriteria(criteriaId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all criteria for a user")
    public ResponseEntity<List<AlertCriteria>> getCriteriaByUserId(
            @Parameter(example = "user-123") @PathVariable String userId) {
        List<AlertCriteria> criteria = queryAlertsUseCase.getCriteriaByUserId(userId);
        return ResponseEntity.ok(criteria);
    }
    
    @GetMapping("/{criteriaId}")
    @Operation(summary = "Get criteria by ID")
    public ResponseEntity<AlertCriteria> getCriteriaById(
            @Parameter(example = "ac8d5d8f-ea03-4df6-bf0a-3f56a41795e6") @PathVariable String criteriaId) {
        AlertCriteria criteria = queryAlertsUseCase.getCriteriaById(criteriaId);
        return ResponseEntity.ok(criteria);
    }
}
