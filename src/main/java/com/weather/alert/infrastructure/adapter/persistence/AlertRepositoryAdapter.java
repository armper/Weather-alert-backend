package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.port.AlertRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
    public List<Alert> findHistoryByCriteriaId(String criteriaId) {
        return jpaRepository.findByCriteriaIdOrderByAlertTimeDesc(criteriaId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Alert> findByCriteriaIdAndEventKey(String criteriaId, String eventKey) {
        return jpaRepository.findByCriteriaIdAndEventKey(criteriaId, eventKey).map(this::toDomain);
    }
    
    @Override
    public List<Alert> findPendingAlerts() {
        return jpaRepository.findByStatus("PENDING").stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Alert> markAsSent(String alertId, Instant sentAt) {
        return jpaRepository.findById(alertId)
                .flatMap(entity -> {
                    Alert.AlertStatus currentStatus = parseStatus(entity.getStatus());
                    if (currentStatus != Alert.AlertStatus.PENDING) {
                        return Optional.empty();
                    }
                    entity.setStatus(Alert.AlertStatus.SENT.name());
                    entity.setSentAt(sentAt);
                    return Optional.of(toDomain(jpaRepository.save(entity)));
                });
    }

    @Override
    public Optional<Alert> acknowledge(String alertId, Instant acknowledgedAt) {
        return jpaRepository.findById(alertId)
                .flatMap(entity -> {
                    Alert.AlertStatus currentStatus = parseStatus(entity.getStatus());
                    if (currentStatus != Alert.AlertStatus.SENT) {
                        return Optional.empty();
                    }
                    entity.setStatus(Alert.AlertStatus.ACKNOWLEDGED.name());
                    entity.setAcknowledgedAt(acknowledgedAt);
                    return Optional.of(toDomain(jpaRepository.save(entity)));
                });
    }

    @Override
    public Optional<Alert> expire(String alertId, Instant expiredAt) {
        return jpaRepository.findById(alertId)
                .flatMap(entity -> {
                    Alert.AlertStatus currentStatus = parseStatus(entity.getStatus());
                    if (currentStatus != Alert.AlertStatus.PENDING && currentStatus != Alert.AlertStatus.SENT) {
                        return Optional.empty();
                    }
                    entity.setStatus(Alert.AlertStatus.EXPIRED.name());
                    entity.setExpiredAt(expiredAt);
                    return Optional.of(toDomain(jpaRepository.save(entity)));
                });
    }

    @Override
    public int deleteByAlertTimeBefore(Instant cutoff) {
        return jpaRepository.deleteByAlertTimeBefore(cutoff);
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
                .eventKey(alert.getEventKey())
                .reason(alert.getReason())
                .eventType(alert.getEventType())
                .severity(alert.getSeverity())
                .headline(alert.getHeadline())
                .description(alert.getDescription())
                .location(alert.getLocation())
                .conditionSource(alert.getConditionSource())
                .conditionOnset(alert.getConditionOnset())
                .conditionExpires(alert.getConditionExpires())
                .conditionTemperatureC(alert.getConditionTemperatureC())
                .conditionHumidity(alert.getConditionHumidity())
                .conditionDewPointC(alert.getConditionDewPointC())
                .conditionWindGust(alert.getConditionWindGust())
                .conditionSkyCover(alert.getConditionSkyCover())
                .conditionPrecipitationProbability(alert.getConditionPrecipitationProbability())
                .conditionPrecipitationAmount(alert.getConditionPrecipitationAmount())
                .conditionRiverGaugeId(alert.getConditionRiverGaugeId())
                .conditionRiverObservedStage(alert.getConditionRiverObservedStage())
                .conditionRiverForecastStage(alert.getConditionRiverForecastStage())
                .conditionRiverFloodStage(alert.getConditionRiverFloodStage())
                .conditionRiverActionStage(alert.getConditionRiverActionStage())
                .conditionRiverObservedCategory(alert.getConditionRiverObservedCategory())
                .conditionRiverForecastCategory(alert.getConditionRiverForecastCategory())
                .conditionRiverStageUnit(alert.getConditionRiverStageUnit())
                .alertTime(alert.getAlertTime())
                .status(alert.getStatus() != null ? alert.getStatus().name() : null)
                .sentAt(alert.getSentAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .expiredAt(alert.getExpiredAt())
                .build();
    }
    
    private Alert toDomain(AlertEntity entity) {
        return Alert.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .criteriaId(entity.getCriteriaId())
                .weatherDataId(entity.getWeatherDataId())
                .eventKey(entity.getEventKey())
                .reason(entity.getReason())
                .eventType(entity.getEventType())
                .severity(entity.getSeverity())
                .headline(entity.getHeadline())
                .description(entity.getDescription())
                .location(entity.getLocation())
                .conditionSource(entity.getConditionSource())
                .conditionOnset(entity.getConditionOnset())
                .conditionExpires(entity.getConditionExpires())
                .conditionTemperatureC(entity.getConditionTemperatureC())
                .conditionHumidity(entity.getConditionHumidity())
                .conditionDewPointC(entity.getConditionDewPointC())
                .conditionWindGust(entity.getConditionWindGust())
                .conditionSkyCover(entity.getConditionSkyCover())
                .conditionPrecipitationProbability(entity.getConditionPrecipitationProbability())
                .conditionPrecipitationAmount(entity.getConditionPrecipitationAmount())
                .conditionRiverGaugeId(entity.getConditionRiverGaugeId())
                .conditionRiverObservedStage(entity.getConditionRiverObservedStage())
                .conditionRiverForecastStage(entity.getConditionRiverForecastStage())
                .conditionRiverFloodStage(entity.getConditionRiverFloodStage())
                .conditionRiverActionStage(entity.getConditionRiverActionStage())
                .conditionRiverObservedCategory(entity.getConditionRiverObservedCategory())
                .conditionRiverForecastCategory(entity.getConditionRiverForecastCategory())
                .conditionRiverStageUnit(entity.getConditionRiverStageUnit())
                .alertTime(entity.getAlertTime())
                .status(entity.getStatus() != null ? Alert.AlertStatus.valueOf(entity.getStatus()) : null)
                .sentAt(entity.getSentAt())
                .acknowledgedAt(entity.getAcknowledgedAt())
                .expiredAt(entity.getExpiredAt())
                .build();
    }

    private Alert.AlertStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return Alert.AlertStatus.PENDING;
        }
        return Alert.AlertStatus.valueOf(status);
    }
}
