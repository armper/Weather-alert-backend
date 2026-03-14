package com.weather.alert.infrastructure.adapter.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.domain.model.TravelPlan;
import com.weather.alert.domain.port.TravelPlanRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TravelPlanRepositoryAdapter implements TravelPlanRepositoryPort {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final JpaTravelPlanRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public TravelPlan save(TravelPlan travelPlan) {
        TravelPlanEntity entity = toEntity(travelPlan);
        TravelPlanEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TravelPlan> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<TravelPlan> findByUserId(String userId) {
        return jpaRepository.findByUserIdOrderByStartDateAscIdAsc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private TravelPlanEntity toEntity(TravelPlan travelPlan) {
        Instant createdAt = travelPlan.getCreatedAt() == null ? Instant.now() : travelPlan.getCreatedAt();
        Instant updatedAt = travelPlan.getUpdatedAt() == null ? createdAt : travelPlan.getUpdatedAt();
        return TravelPlanEntity.builder()
                .id(travelPlan.getId())
                .userId(travelPlan.getUserId())
                .name(travelPlan.getName())
                .destination(travelPlan.getDestination())
                .startDate(travelPlan.getStartDate())
                .endDate(travelPlan.getEndDate())
                .latitude(travelPlan.getLatitude())
                .longitude(travelPlan.getLongitude())
                .notes(travelPlan.getNotes())
                .alertsEnabled(travelPlan.getAlertsEnabled() == null || travelPlan.getAlertsEnabled())
                .alertCoverageMode(travelPlan.getAlertCoverageMode() == null ? "ALL_ALERTS" : travelPlan.getAlertCoverageMode())
                .selectedAlertTopics(serializeList(travelPlan.getSelectedAlertTopics()))
                .linkedCriteriaIds(serializeList(travelPlan.getLinkedCriteriaIds()))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private TravelPlan toDomain(TravelPlanEntity entity) {
        return TravelPlan.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .destination(entity.getDestination())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .notes(entity.getNotes())
                .alertsEnabled(entity.getAlertsEnabled())
                .alertCoverageMode(entity.getAlertCoverageMode() == null ? "ALL_ALERTS" : entity.getAlertCoverageMode())
                .selectedAlertTopics(deserializeList(entity.getSelectedAlertTopics()))
                .linkedCriteriaIds(deserializeList(entity.getLinkedCriteriaIds()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String serializeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize travel plan list field", exception);
        }
    }

    private List<String> deserializeList(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawValue, STRING_LIST);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to deserialize travel plan list field", exception);
        }
    }
}
