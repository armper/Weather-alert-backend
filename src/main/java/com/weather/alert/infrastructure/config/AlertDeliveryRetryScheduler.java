package com.weather.alert.infrastructure.config;

import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.port.AlertDeliveryRepositoryPort;
import com.weather.alert.domain.port.AlertDeliveryTaskPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertDeliveryRetryScheduler {

    private final AlertDeliveryRepositoryPort alertDeliveryRepository;
    private final AlertDeliveryTaskPublisherPort taskPublisher;
    private final NotificationDeliveryProperties properties;

    @Scheduled(
            fixedDelayString = "${app.notification.delivery.retry-poller-fixed-delay-ms:10000}",
            initialDelayString = "${app.notification.delivery.retry-poller-initial-delay-ms:15000}")
    public void publishDueTasks() {
        if (!properties.isWorkerEnabled()) {
            return;
        }
        List<AlertDeliveryRecord> due = alertDeliveryRepository.findDueForDelivery(
                Instant.now(),
                properties.getRetryPollerBatchSize());
        if (due.isEmpty()) {
            return;
        }
        due.forEach(record -> taskPublisher.publishTask(record.getId()));
        log.info("Published {} due alert delivery tasks", due.size());
    }
}
