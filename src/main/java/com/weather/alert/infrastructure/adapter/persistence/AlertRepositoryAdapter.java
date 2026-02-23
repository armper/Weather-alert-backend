package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.port.AlertRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AlertRepositoryAdapter implements AlertRepositoryPort {
    
    private final JpaAlertRepository jpaRepository;
    
    @Override
    public Alert save(Alert alert) {
        AlertEntity entity = toEntity(alert);
        AlertEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public Optional<Alert> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }
    
    @Override
    public List<Alert> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Alert> findPendingAlerts() {
        return jpaRepository.findByStatus("PENDING").stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void delete(String id) {
        jpaRepository.deleteById(id);
    }
    
    private AlertEntity toEntity(Alert alert) {
        return AlertEntity.builder()
                .id(alert.getId())
                .userId(alert.getUserId())
                .criteriaId(alert.getCriteriaId())
                .weatherDataId(alert.getWeatherDataId())
                .eventType(alert.getEventType())
                .severity(alert.getSeverity())
                .headline(alert.getHeadline())
                .description(alert.getDescription())
                .location(alert.getLocation())
                .alertTime(alert.getAlertTime())
                .status(alert.getStatus() != null ? alert.getStatus().name() : null)
                .sentAt(alert.getSentAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .build();
    }
    
    private Alert toDomain(AlertEntity entity) {
        return Alert.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .criteriaId(entity.getCriteriaId())
                .weatherDataId(entity.getWeatherDataId())
                .eventType(entity.getEventType())
                .severity(entity.getSeverity())
                .headline(entity.getHeadline())
                .description(entity.getDescription())
                .location(entity.getLocation())
                .alertTime(entity.getAlertTime())
                .status(entity.getStatus() != null ? Alert.AlertStatus.valueOf(entity.getStatus()) : null)
                .sentAt(entity.getSentAt())
                .acknowledgedAt(entity.getAcknowledgedAt())
                .build();
    }
}
