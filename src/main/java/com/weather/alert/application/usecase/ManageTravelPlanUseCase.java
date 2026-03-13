package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.CreateTravelPlanRequest;
import com.weather.alert.application.dto.TravelPlanResponse;
import com.weather.alert.application.exception.TravelPlanNotFoundException;
import com.weather.alert.domain.model.TravelPlan;
import com.weather.alert.domain.port.TravelPlanRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use case for managing user travel plans.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManageTravelPlanUseCase {

    private final TravelPlanRepositoryPort travelPlanRepository;

    public TravelPlan createPlan(String userId, CreateTravelPlanRequest request) {
        TravelPlan plan = TravelPlan.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .name(request.getName())
                .destination(request.getDestination())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .notes(request.getNotes())
                .alertsEnabled(request.getAlertsEnabled() == null ? true : request.getAlertsEnabled())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        TravelPlan saved = travelPlanRepository.save(plan);
        log.info("Created travel plan {} for user {}", saved.getId(), userId);
        return saved;
    }

    public TravelPlan updatePlan(String planId, CreateTravelPlanRequest request) {
        TravelPlan existing = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new TravelPlanNotFoundException(planId));
        TravelPlan updated = TravelPlan.builder()
                .id(existing.getId())
                .userId(existing.getUserId())
                .name(request.getName())
                .destination(request.getDestination())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .notes(request.getNotes())
                .alertsEnabled(request.getAlertsEnabled() == null ? existing.getAlertsEnabled() : request.getAlertsEnabled())
                .createdAt(existing.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
        TravelPlan saved = travelPlanRepository.save(updated);
        log.info("Updated travel plan {} for user {}", saved.getId(), existing.getUserId());
        return saved;
    }

    public List<TravelPlan> getPlansForUser(String userId) {
        return travelPlanRepository.findByUserId(userId);
    }

    public TravelPlan getPlanById(String planId) {
        return travelPlanRepository.findById(planId)
                .orElseThrow(() -> new TravelPlanNotFoundException(planId));
    }

    public void deletePlan(String planId) {
        travelPlanRepository.findById(planId)
                .orElseThrow(() -> new TravelPlanNotFoundException(planId));
        travelPlanRepository.delete(planId);
        log.info("Deleted travel plan {}", planId);
    }
}
