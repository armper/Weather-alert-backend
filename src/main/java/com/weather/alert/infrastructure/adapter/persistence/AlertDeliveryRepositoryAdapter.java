package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.AlertDeliveryStatus;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.port.AlertDeliveryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AlertDeliveryRepositoryAdapter implements AlertDeliveryRepositoryPort {

    private final JpaAlertDeliveryRepository jpaRepository;

    @Override
    public AlertDeliveryRecord save(AlertDeliveryRecord deliveryRecord) {
        AlertDeliveryEntity entity = toEntity(deliveryRecord);
        AlertDeliveryEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<AlertDeliveryRecord> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<AlertDeliveryRecord> findByAlertIdAndChannel(String alertId, NotificationChannel channel) {
        return jpaRepository.findByAlertIdAndChannel(alertId, channel.name()).map(this::toDomain);
    }

    @Override
    public List<AlertDeliveryRecord> findDueForDelivery(Instant now, int limit) {
        int safeLimit = Math.max(limit, 1);
        List<AlertDeliveryEntity> entities = jpaRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                List.of(AlertDeliveryStatus.PENDING.name(), AlertDeliveryStatus.RETRY_SCHEDULED.name()),
                now,
                PageRequest.of(0, safeLimit));
        return entities.stream().map(this::toDomain).toList();
    }

    private AlertDeliveryEntity toEntity(AlertDeliveryRecord deliveryRecord) {
        return AlertDeliveryEntity.builder()
                .id(deliveryRecord.getId())
                .alertId(deliveryRecord.getAlertId())
                .userId(deliveryRecord.getUserId())
                .channel(deliveryRecord.getChannel().name())
                .destination(deliveryRecord.getDestination())
                .status(deliveryRecord.getStatus().name())
                .attemptCount(deliveryRecord.getAttemptCount() == null ? 0 : deliveryRecord.getAttemptCount())
                .lastError(deliveryRecord.getLastError())
                .providerMessageId(deliveryRecord.getProviderMessageId())
                .sentAt(deliveryRecord.getSentAt())
                .nextAttemptAt(deliveryRecord.getNextAttemptAt())
                .createdAt(deliveryRecord.getCreatedAt())
                .updatedAt(deliveryRecord.getUpdatedAt())
                .build();
    }

    private AlertDeliveryRecord toDomain(AlertDeliveryEntity entity) {
        return AlertDeliveryRecord.builder()
                .id(entity.getId())
                .alertId(entity.getAlertId())
                .userId(entity.getUserId())
                .channel(NotificationChannel.valueOf(entity.getChannel()))
                .destination(entity.getDestination())
                .status(AlertDeliveryStatus.valueOf(entity.getStatus()))
                .attemptCount(entity.getAttemptCount())
                .lastError(entity.getLastError())
                .providerMessageId(entity.getProviderMessageId())
                .sentAt(entity.getSentAt())
                .nextAttemptAt(entity.getNextAttemptAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
