package com.weather.alert.infrastructure.config;

import com.weather.alert.application.usecase.PublishDueAlertDeliveryTasksUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlertDeliveryRetrySchedulerTest {

    @Mock
    private PublishDueAlertDeliveryTasksUseCase publishDueAlertDeliveryTasksUseCase;

    private AlertDeliveryRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AlertDeliveryRetryScheduler(publishDueAlertDeliveryTasksUseCase);
    }

    @Test
    void shouldDelegateToUseCase() {
        scheduler.publishDueTasks();

        verify(publishDueAlertDeliveryTasksUseCase).run();
    }
}
