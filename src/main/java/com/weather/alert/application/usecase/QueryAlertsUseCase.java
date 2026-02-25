package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.AlertCriteriaQueryFilter;
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

    public List<AlertCriteria> getCriteriaByUserId(String userId, AlertCriteriaQueryFilter filter) {
        List<AlertCriteria> criteria = criteriaRepository.findByUserId(userId);
        if (filter == null) {
            return criteria;
        }
        return criteria.stream()
                .filter(item -> filter.getTemperatureUnit() == null || item.getTemperatureUnit() == filter.getTemperatureUnit())
                .filter(item -> filter.getMonitorCurrent() == null || filter.getMonitorCurrent().equals(item.getMonitorCurrent()))
                .filter(item -> filter.getMonitorForecast() == null || filter.getMonitorForecast().equals(item.getMonitorForecast()))
                .filter(item -> filter.getEnabled() == null || filter.getEnabled().equals(item.getEnabled()))
                .filter(item -> filter.getHasTemperatureRule() == null || filter.getHasTemperatureRule().equals(hasTemperatureRule(item)))
                .filter(item -> filter.getHasRainRule() == null || filter.getHasRainRule().equals(hasRainRule(item)))
                .toList();
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

    private boolean hasTemperatureRule(AlertCriteria criteria) {
        return criteria.getTemperatureThreshold() != null
                || criteria.getMaxTemperature() != null
                || criteria.getMinTemperature() != null;
    }

    private boolean hasRainRule(AlertCriteria criteria) {
        return criteria.getRainThreshold() != null
                || criteria.getMaxPrecipitation() != null;
    }
}
