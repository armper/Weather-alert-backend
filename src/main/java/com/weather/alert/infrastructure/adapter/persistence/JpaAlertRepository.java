package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaAlertRepository extends JpaRepository<AlertEntity, String> {
    List<AlertEntity> findByUserId(String userId);
    List<AlertEntity> findByCriteriaIdOrderByAlertTimeDesc(String criteriaId);
    Optional<AlertEntity> findByCriteriaIdAndEventKey(String criteriaId, String eventKey);
    List<AlertEntity> findByStatus(String status);
}
