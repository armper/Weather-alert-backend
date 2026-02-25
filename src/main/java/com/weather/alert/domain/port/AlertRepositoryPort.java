package com.weather.alert.domain.port;

import com.weather.alert.domain.model.Alert;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port for alert persistence
 */
public interface AlertRepositoryPort {
    
    Alert save(Alert alert);
    
    Optional<Alert> findById(String id);
    
    List<Alert> findByUserId(String userId);

    List<Alert> findHistoryByCriteriaId(String criteriaId);

    Optional<Alert> findByCriteriaIdAndEventKey(String criteriaId, String eventKey);
    
    List<Alert> findPendingAlerts();

    Optional<Alert> markAsSent(String alertId, Instant sentAt);

    Optional<Alert> acknowledge(String alertId, Instant acknowledgedAt);

    Optional<Alert> expire(String alertId, Instant expiredAt);
    
    void delete(String id);
}
