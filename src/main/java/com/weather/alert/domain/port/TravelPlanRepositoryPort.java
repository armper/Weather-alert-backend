package com.weather.alert.domain.port;

import com.weather.alert.domain.model.TravelPlan;

import java.util.List;
import java.util.Optional;

public interface TravelPlanRepositoryPort {

    TravelPlan save(TravelPlan travelPlan);

    Optional<TravelPlan> findById(String id);

    List<TravelPlan> findByUserId(String userId);

    void deleteById(String id);
}
