package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.TravelPlan;
import com.weather.alert.domain.port.TravelPlanRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TravelPlanRepositoryAdapter implements TravelPlanRepositoryPort {

    private final JpaTravelPlanRepository jpaRepository;

    @Override
    public TravelPlan save(TravelPlan plan) {
        TravelPlanEntity entity = toEntity(plan);
        TravelPlanEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TravelPlan> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<TravelPlan> findByUserId(String userId) {
        return jpaRepository.findByUserIdOrderByStartDateAscCreatedAtAsc(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        jpaRepository.deleteById(id);
    }

    private TravelPlanEntity toEntity(TravelPlan plan) {
        Instant now = Instant.now();
        return TravelPlanEntity.builder()
                .id(plan.getId())
                .userId(plan.getUserId())
                .name(plan.getName())
                .destination(plan.getDestination())
                .latitude(plan.getLatitude())
                .longitude(plan.getLongitude())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .notes(plan.getNotes())
                .alertsEnabled(plan.getAlertsEnabled())
                .createdAt(plan.getCreatedAt() == null ? now : plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt() == null ? now : plan.getUpdatedAt())
                .build();
    }

    private TravelPlan toDomain(TravelPlanEntity entity) {
        return TravelPlan.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .destination(entity.getDestination())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .notes(entity.getNotes())
                .alertsEnabled(entity.getAlertsEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
