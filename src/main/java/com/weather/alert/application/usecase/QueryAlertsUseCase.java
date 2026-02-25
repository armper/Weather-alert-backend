package com.weather.alert.application.usecase;

import com.weather.alert.application.exception.AlertNotFoundException;
import com.weather.alert.application.exception.CriteriaNotFoundException;
import com.weather.alert.application.exception.InvalidAlertTransitionException;
import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.AlertRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    public List<Alert> getAlertHistoryByCriteriaId(String criteriaId) {
        return alertRepository.findHistoryByCriteriaId(criteriaId);
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

    public Alert acknowledgeAlert(String alertId) {
        Alert current = getAlertById(alertId);
        return alertRepository.acknowledge(alertId, Instant.now())
                .orElseThrow(() -> new InvalidAlertTransitionException(
                        alertId,
                        current.getStatus() != null ? current.getStatus().name() : "UNKNOWN",
                        Alert.AlertStatus.ACKNOWLEDGED.name()
                ));
    }

    public Alert expireAlert(String alertId) {
        Alert current = getAlertById(alertId);
        return alertRepository.expire(alertId, Instant.now())
                .orElseThrow(() -> new InvalidAlertTransitionException(
                        alertId,
                        current.getStatus() != null ? current.getStatus().name() : "UNKNOWN",
                        Alert.AlertStatus.EXPIRED.name()
                ));
    }
}
