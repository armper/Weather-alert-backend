package com.weather.alert.domain.port;

import com.weather.alert.domain.model.Alert;

import java.util.List;
import java.util.Optional;

/**
 * Port for alert persistence
 */
public interface AlertRepositoryPort {
    
    Alert save(Alert alert);
    
    Optional<Alert> findById(String id);
    
    List<Alert> findByUserId(String userId);
    
    List<Alert> findPendingAlerts();
    
    void delete(String id);
}
