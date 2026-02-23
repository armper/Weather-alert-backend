package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
import com.weather.alert.application.usecase.ManageAlertCriteriaUseCase;
import com.weather.alert.application.usecase.QueryAlertsUseCase;
import com.weather.alert.domain.model.AlertCriteria;
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
public class AlertCriteriaController {
    
    private final ManageAlertCriteriaUseCase manageAlertCriteriaUseCase;
    private final QueryAlertsUseCase queryAlertsUseCase;
    
    @PostMapping
    public ResponseEntity<AlertCriteria> createCriteria(@RequestBody CreateAlertCriteriaRequest request) {
        AlertCriteria criteria = manageAlertCriteriaUseCase.createCriteria(request);
        return ResponseEntity.ok(criteria);
    }
    
    @PutMapping("/{criteriaId}")
    public ResponseEntity<AlertCriteria> updateCriteria(
            @PathVariable String criteriaId,
            @RequestBody CreateAlertCriteriaRequest request) {
        AlertCriteria criteria = manageAlertCriteriaUseCase.updateCriteria(criteriaId, request);
        return ResponseEntity.ok(criteria);
    }
    
    @DeleteMapping("/{criteriaId}")
    public ResponseEntity<Void> deleteCriteria(@PathVariable String criteriaId) {
        manageAlertCriteriaUseCase.deleteCriteria(criteriaId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AlertCriteria>> getCriteriaByUserId(@PathVariable String userId) {
        List<AlertCriteria> criteria = queryAlertsUseCase.getCriteriaByUserId(userId);
        return ResponseEntity.ok(criteria);
    }
    
    @GetMapping("/{criteriaId}")
    public ResponseEntity<AlertCriteria> getCriteriaById(@PathVariable String criteriaId) {
        AlertCriteria criteria = queryAlertsUseCase.getCriteriaById(criteriaId);
        return ResponseEntity.ok(criteria);
    }
}
