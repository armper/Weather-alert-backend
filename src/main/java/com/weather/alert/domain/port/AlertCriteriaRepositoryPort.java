package com.weather.alert.domain.port;

import com.weather.alert.domain.model.AlertCriteria;

import java.util.List;
import java.util.Optional;

/**
 * Port for alert criteria persistence
 */
public interface AlertCriteriaRepositoryPort {
    
    AlertCriteria save(AlertCriteria criteria);
    
    Optional<AlertCriteria> findById(String id);
    
    List<AlertCriteria> findByUserId(String userId);
    
    List<AlertCriteria> findAllEnabled();
    
    void delete(String id);
}
