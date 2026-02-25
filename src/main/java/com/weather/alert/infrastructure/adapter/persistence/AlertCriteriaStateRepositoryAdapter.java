package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.AlertCriteriaState;
import com.weather.alert.domain.port.AlertCriteriaStateRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AlertCriteriaStateRepositoryAdapter implements AlertCriteriaStateRepositoryPort {

    private final JpaAlertCriteriaStateRepository jpaRepository;

    @Override
    public Optional<AlertCriteriaState> findByCriteriaId(String criteriaId) {
        return jpaRepository.findById(criteriaId).map(this::toDomain);
    }

    @Override
    public AlertCriteriaState save(AlertCriteriaState state) {
        AlertCriteriaStateEntity entity = toEntity(state);
        AlertCriteriaStateEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private AlertCriteriaStateEntity toEntity(AlertCriteriaState state) {
        return AlertCriteriaStateEntity.builder()
                .criteriaId(state.getCriteriaId())
                .lastConditionMet(state.getLastConditionMet())
                .lastEventSignature(state.getLastEventSignature())
                .lastNotifiedAt(state.getLastNotifiedAt())
                .createdAt(state.getCreatedAt())
                .updatedAt(state.getUpdatedAt())
                .build();
    }

    private AlertCriteriaState toDomain(AlertCriteriaStateEntity entity) {
        return AlertCriteriaState.builder()
                .criteriaId(entity.getCriteriaId())
                .lastConditionMet(entity.getLastConditionMet())
                .lastEventSignature(entity.getLastEventSignature())
                .lastNotifiedAt(entity.getLastNotifiedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
