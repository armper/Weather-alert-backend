package com.weather.alert.domain.port;

import com.weather.alert.domain.model.TravelPlan;

import java.util.List;
import java.util.Optional;

/**
 * Port for travel plan persistence.
 */
public interface TravelPlanRepositoryPort {

    TravelPlan save(TravelPlan plan);

    Optional<TravelPlan> findById(String id);

    List<TravelPlan> findByUserId(String userId);

    void delete(String id);
}
