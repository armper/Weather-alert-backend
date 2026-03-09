package com.weather.alert.infrastructure.config;

import com.weather.alert.application.usecase.PublishDueAlertDeliveryTasksUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        value = "app.notification.delivery.retry-poller-enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class AlertDeliveryRetryScheduler {

    private final PublishDueAlertDeliveryTasksUseCase publishDueAlertDeliveryTasksUseCase;

    @Scheduled(
            fixedDelayString = "${app.notification.delivery.retry-poller-fixed-delay-ms:10000}",
            initialDelayString = "${app.notification.delivery.retry-poller-initial-delay-ms:15000}")
    public void publishDueTasks() {
        publishDueAlertDeliveryTasksUseCase.run();
    }
}
