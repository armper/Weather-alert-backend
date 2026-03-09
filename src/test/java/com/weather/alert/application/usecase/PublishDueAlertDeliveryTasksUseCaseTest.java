package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.JobRunResponse;
import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.port.AlertDeliveryRepositoryPort;
import com.weather.alert.domain.port.AlertDeliveryTaskPublisherPort;
import com.weather.alert.infrastructure.config.NotificationDeliveryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishDueAlertDeliveryTasksUseCaseTest {

    @Mock
    private AlertDeliveryRepositoryPort alertDeliveryRepository;

    @Mock
    private AlertDeliveryTaskPublisherPort taskPublisher;

    private NotificationDeliveryProperties properties;
    private PublishDueAlertDeliveryTasksUseCase useCase;

    @BeforeEach
    void setUp() {
        properties = new NotificationDeliveryProperties();
        properties.setWorkerEnabled(true);
        properties.setRetryPollerBatchSize(10);
        useCase = new PublishDueAlertDeliveryTasksUseCase(alertDeliveryRepository, taskPublisher, properties);
    }

    @Test
    void shouldPublishDueTasks() {
        when(alertDeliveryRepository.findDueForDelivery(any(Instant.class), eq(10)))
                .thenReturn(List.of(
                        AlertDeliveryRecord.builder().id("delivery-1").build(),
                        AlertDeliveryRecord.builder().id("delivery-2").build()));

        JobRunResponse response = useCase.run();

        verify(taskPublisher).publishTask("delivery-1");
        verify(taskPublisher).publishTask("delivery-2");
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(2L, response.getMetrics().get("publishedCount"));
    }

    @Test
    void shouldSkipWhenWorkerDisabled() {
        properties.setWorkerEnabled(false);

        JobRunResponse response = useCase.run();

        verify(alertDeliveryRepository, never()).findDueForDelivery(any(Instant.class), eq(10));
        verify(taskPublisher, never()).publishTask(any(String.class));
        assertEquals("SKIPPED", response.getStatus());
        assertEquals("delivery worker disabled", response.getMessage());
    }
}
