package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaTravelPlanRepository extends JpaRepository<TravelPlanEntity, String> {

    List<TravelPlanEntity> findByUserIdOrderByStartDateAscIdAsc(String userId);
}
