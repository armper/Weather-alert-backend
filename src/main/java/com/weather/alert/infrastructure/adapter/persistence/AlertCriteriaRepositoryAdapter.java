package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AlertCriteriaRepositoryAdapter implements AlertCriteriaRepositoryPort {
    
    private final JpaAlertCriteriaRepository jpaRepository;
    
    @Override
    public AlertCriteria save(AlertCriteria criteria) {
        AlertCriteriaEntity entity = toEntity(criteria);
        AlertCriteriaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public Optional<AlertCriteria> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }
    
    @Override
    public List<AlertCriteria> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AlertCriteria> findAllEnabled() {
        return jpaRepository.findByEnabled(true).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void delete(String id) {
        jpaRepository.deleteById(id);
    }
    
    private AlertCriteriaEntity toEntity(AlertCriteria criteria) {
        return AlertCriteriaEntity.builder()
                .id(criteria.getId())
                .userId(criteria.getUserId())
                .location(criteria.getLocation())
                .latitude(criteria.getLatitude())
                .longitude(criteria.getLongitude())
                .radiusKm(criteria.getRadiusKm())
                .eventType(criteria.getEventType())
                .minSeverity(criteria.getMinSeverity())
                .maxTemperature(criteria.getMaxTemperature())
                .minTemperature(criteria.getMinTemperature())
                .maxWindSpeed(criteria.getMaxWindSpeed())
                .maxPrecipitation(criteria.getMaxPrecipitation())
                .enabled(criteria.getEnabled())
                .build();
    }
    
    private AlertCriteria toDomain(AlertCriteriaEntity entity) {
        return AlertCriteria.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .location(entity.getLocation())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .radiusKm(entity.getRadiusKm())
                .eventType(entity.getEventType())
                .minSeverity(entity.getMinSeverity())
                .maxTemperature(entity.getMaxTemperature())
                .minTemperature(entity.getMinTemperature())
                .maxWindSpeed(entity.getMaxWindSpeed())
                .maxPrecipitation(entity.getMaxPrecipitation())
                .enabled(entity.getEnabled())
                .build();
    }
}
