package com.weather.alert.infrastructure.config;

import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.port.AlertDeliveryRepositoryPort;
import com.weather.alert.domain.port.AlertDeliveryTaskPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertDeliveryRetrySchedulerTest {

    @Mock
    private AlertDeliveryRepositoryPort alertDeliveryRepository;

    @Mock
    private AlertDeliveryTaskPublisherPort taskPublisher;

    private NotificationDeliveryProperties properties;
    private AlertDeliveryRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new NotificationDeliveryProperties();
        properties.setWorkerEnabled(true);
        properties.setRetryPollerBatchSize(10);
        scheduler = new AlertDeliveryRetryScheduler(alertDeliveryRepository, taskPublisher, properties);
    }

    @Test
    void shouldPublishDueTasks() {
        when(alertDeliveryRepository.findDueForDelivery(any(Instant.class), eq(10)))
                .thenReturn(List.of(
                        AlertDeliveryRecord.builder().id("delivery-1").build(),
                        AlertDeliveryRecord.builder().id("delivery-2").build()));

        scheduler.publishDueTasks();

        verify(taskPublisher).publishTask("delivery-1");
        verify(taskPublisher).publishTask("delivery-2");
    }

    @Test
    void shouldSkipWhenWorkerDisabled() {
        properties.setWorkerEnabled(false);

        scheduler.publishDueTasks();

        verify(alertDeliveryRepository, never()).findDueForDelivery(any(Instant.class), eq(10));
        verify(taskPublisher, never()).publishTask(any(String.class));
    }
}
