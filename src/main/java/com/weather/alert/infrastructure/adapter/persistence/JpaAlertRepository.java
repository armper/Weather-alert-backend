package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaAlertRepository extends JpaRepository<AlertEntity, String> {
    List<AlertEntity> findByUserId(String userId);
    List<AlertEntity> findByStatus(String status);
}
