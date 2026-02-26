package com.weather.alert.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.AlertDeliveryStatus;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.ResolvedNotificationPreference;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.AlertDeliveryRepositoryPort;
import com.weather.alert.domain.port.AlertDeliveryTaskPublisherPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import com.weather.alert.domain.service.notification.InvalidNotificationPreferenceConfigurationException;
import com.weather.alert.domain.service.notification.NotificationPreferenceResolverService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnqueueAlertDeliveryUseCase {

    private final AlertDeliveryRepositoryPort alertDeliveryRepository;
    private final NotificationPreferenceResolverService notificationPreferenceResolverService;
    private final UserRepositoryPort userRepository;
    private final AlertDeliveryTaskPublisherPort taskPublisher;

    @Transactional
    public void enqueue(Alert alert) {
        if (alert == null || alert.getId() == null || alert.getId().isBlank()
                || alert.getUserId() == null || alert.getUserId().isBlank()) {
            return;
        }

        User user = userRepository.findById(alert.getUserId()).orElse(null);
        if (user == null) {
            log.warn("Skipping delivery enqueue for alert {}: user {} not found", alert.getId(), alert.getUserId());
            return;
        }

        ResolvedNotificationPreference resolved;
        try {
            resolved = notificationPreferenceResolverService.resolve(alert.getUserId(), alert.getCriteriaId());
        } catch (InvalidNotificationPreferenceConfigurationException ex) {
            log.warn("Skipping delivery enqueue for alert {} due to notification config: {}", alert.getId(), ex.getMessage());
            return;
        }

        for (NotificationChannel channel : resolved.getOrderedChannels()) {
            String destination = resolveDestination(channel, user);
            if (destination == null || destination.isBlank()) {
                log.warn(
                        "Skipping delivery enqueue for alert {} channel {}: destination missing",
                        alert.getId(),
                        channel);
            } else {
                AlertDeliveryRecord existing = alertDeliveryRepository.findByAlertIdAndChannel(alert.getId(), channel).orElse(null);
                if (existing != null) {
                    if (existing.getStatus() == AlertDeliveryStatus.PENDING || existing.getStatus() == AlertDeliveryStatus.RETRY_SCHEDULED) {
                        taskPublisher.publishTask(existing.getId());
                    }
                } else {
                    Instant now = Instant.now();
                    AlertDeliveryRecord created = alertDeliveryRepository.save(AlertDeliveryRecord.builder()
                            .id(UUID.randomUUID().toString())
                            .alertId(alert.getId())
                            .userId(alert.getUserId())
                            .channel(channel)
                            .destination(destination)
                            .status(AlertDeliveryStatus.PENDING)
                            .attemptCount(0)
                            .nextAttemptAt(now)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                    taskPublisher.publishTask(created.getId());
                }
            }
        }
    }

    private String resolveDestination(NotificationChannel channel, User user) {
        if (channel == NotificationChannel.EMAIL) {
            return user.getEmail();
        }
        if (channel == NotificationChannel.SMS) {
            return user.getPhoneNumber();
        }
        return user.getId();
    }
}
