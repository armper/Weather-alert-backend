package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaAlertCriteriaRepository extends JpaRepository<AlertCriteriaEntity, String> {
    List<AlertCriteriaEntity> findByUserId(String userId);
    List<AlertCriteriaEntity> findByEnabled(Boolean enabled);
}
