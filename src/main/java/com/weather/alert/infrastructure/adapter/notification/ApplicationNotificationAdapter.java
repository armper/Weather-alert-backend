package com.weather.alert.infrastructure.adapter.notification;

import com.weather.alert.application.usecase.EnqueueAlertDeliveryUseCase;
import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationNotificationAdapter implements NotificationPort {

    private final EnqueueAlertDeliveryUseCase enqueueAlertDeliveryUseCase;

    @Override
    public void sendAlert(Alert alert, String userId) {
        if (alert != null && (alert.getUserId() == null || alert.getUserId().isBlank())) {
            alert.setUserId(userId);
        }
        publishAlert(alert);
    }

    @Override
    public void publishAlert(Alert alert) {
        if (alert == null) {
            return;
        }
        try {
            enqueueAlertDeliveryUseCase.enqueue(alert);
        } catch (Exception ex) {
            log.error("Failed to enqueue delivery work for alert {}", alert.getId(), ex);
        }
    }
}
