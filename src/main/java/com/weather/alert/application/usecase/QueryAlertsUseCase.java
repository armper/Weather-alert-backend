package com.weather.alert.application.usecase;

import com.weather.alert.application.exception.AlertNotFoundException;
import com.weather.alert.application.exception.CriteriaNotFoundException;
import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.AlertRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for querying alerts and criteria (Query - CQRS)
 */
@Service
@RequiredArgsConstructor
public class QueryAlertsUseCase {
    
    private final AlertRepositoryPort alertRepository;
    private final AlertCriteriaRepositoryPort criteriaRepository;
    
    public List<Alert> getAlertsByUserId(String userId) {
        return alertRepository.findByUserId(userId);
    }
    
    public Alert getAlertById(String alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
    }
    
    public List<AlertCriteria> getCriteriaByUserId(String userId) {
        return criteriaRepository.findByUserId(userId);
    }
    
    public AlertCriteria getCriteriaById(String criteriaId) {
        return criteriaRepository.findById(criteriaId)
                .orElseThrow(() -> new CriteriaNotFoundException(criteriaId));
    }
    
    public List<Alert> getPendingAlerts() {
        return alertRepository.findPendingAlerts();
    }
}
