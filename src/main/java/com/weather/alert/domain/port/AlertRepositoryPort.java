package com.weather.alert.domain.port;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.PagedResult;

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

    PagedResult<Alert> findByUserIdPaged(String userId, int page, int size);

    List<Alert> findHistoryByCriteriaId(String criteriaId);

    PagedResult<Alert> findHistoryByCriteriaIdPaged(String criteriaId, int page, int size);

    Optional<Alert> findByCriteriaIdAndEventKey(String criteriaId, String eventKey);
    
    List<Alert> findPendingAlerts();

    Optional<Alert> markAsSent(String alertId, Instant sentAt);

    Optional<Alert> acknowledge(String alertId, Instant acknowledgedAt);

    Optional<Alert> expire(String alertId, Instant expiredAt);

    int deleteByAlertTimeBefore(Instant cutoff);
    
    void delete(String id);
}
