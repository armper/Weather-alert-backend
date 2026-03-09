package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.JobRunResponse;
import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.port.AlertDeliveryRepositoryPort;
import com.weather.alert.domain.port.AlertDeliveryTaskPublisherPort;
import com.weather.alert.infrastructure.config.NotificationDeliveryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublishDueAlertDeliveryTasksUseCase {

    private final AlertDeliveryRepositoryPort alertDeliveryRepository;
    private final AlertDeliveryTaskPublisherPort taskPublisher;
    private final NotificationDeliveryProperties properties;

    public JobRunResponse run() {
        Instant startedAt = Instant.now();
        if (!properties.isWorkerEnabled()) {
            return completed(startedAt, "SKIPPED", 0, "delivery worker disabled");
        }

        List<AlertDeliveryRecord> due = alertDeliveryRepository.findDueForDelivery(
                Instant.now(),
                properties.getRetryPollerBatchSize());
        due.forEach(record -> taskPublisher.publishTask(record.getId()));
        log.info("Published {} due alert delivery tasks", due.size());
        return completed(startedAt, "COMPLETED", due.size(), null);
    }

    private JobRunResponse completed(Instant startedAt, String status, int publishedCount, String reason) {
        Instant finishedAt = Instant.now();
        long durationMillis = Duration.between(startedAt, finishedAt).toMillis();
        return JobRunResponse.builder()
                .jobName("alert-delivery-retries")
                .status(status)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .durationMillis(durationMillis)
                .message(reason)
                .metrics(Map.of("publishedCount", (long) publishedCount))
                .build();
    }
}
