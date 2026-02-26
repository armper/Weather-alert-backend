package com.weather.alert.application.usecase;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.AlertDeliveryStatus;
import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.EmailSendResult;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.port.AlertDeliveryDlqPublisherPort;
import com.weather.alert.domain.port.AlertDeliveryRepositoryPort;
import com.weather.alert.domain.port.AlertRepositoryPort;
import com.weather.alert.domain.port.EmailSenderPort;
import com.weather.alert.domain.service.notification.EmailDeliveryException;
import com.weather.alert.infrastructure.config.NotificationDeliveryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessAlertDeliveryTaskUseCase {

    private final AlertDeliveryRepositoryPort alertDeliveryRepository;
    private final AlertRepositoryPort alertRepository;
    private final EmailSenderPort emailSenderPort;
    private final AlertDeliveryDlqPublisherPort dlqPublisher;
    private final NotificationDeliveryProperties properties;

    @Transactional
    public void processTask(String deliveryId) {
        AlertDeliveryRecord delivery = alertDeliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null) {
            return;
        }

        Instant now = Instant.now();
        if (delivery.getStatus() == AlertDeliveryStatus.SENT || delivery.getStatus() == AlertDeliveryStatus.FAILED) {
            return;
        }
        if (delivery.getNextAttemptAt() != null && delivery.getNextAttemptAt().isAfter(now)) {
            return;
        }

        delivery.setStatus(AlertDeliveryStatus.IN_PROGRESS);
        delivery.setUpdatedAt(now);
        alertDeliveryRepository.save(delivery);

        int attempt = normalizeAttempts(delivery) + 1;
        try {
            EmailSendResult result = sendForChannel(delivery);
            delivery.setAttemptCount(attempt);
            delivery.setStatus(AlertDeliveryStatus.SENT);
            delivery.setProviderMessageId(result == null ? null : result.providerMessageId());
            delivery.setSentAt(now);
            delivery.setLastError(null);
            delivery.setNextAttemptAt(null);
            delivery.setUpdatedAt(now);
            alertDeliveryRepository.save(delivery);
            alertRepository.markAsSent(delivery.getAlertId(), now);
        } catch (EmailDeliveryException ex) {
            handleFailure(delivery, attempt, ex.getFailureType(), ex.getMessage(), now, ex);
        } catch (Exception ex) {
            handleFailure(delivery, attempt, DeliveryFailureType.RETRYABLE, ex.getMessage(), now, ex);
        }
    }

    private EmailSendResult sendForChannel(AlertDeliveryRecord delivery) {
        if (delivery.getChannel() != NotificationChannel.EMAIL) {
            throw new EmailDeliveryException(
                    DeliveryFailureType.NON_RETRYABLE,
                    "Channel " + delivery.getChannel() + " is not yet supported by delivery worker",
                    null);
        }
        Alert alert = alertRepository.findById(delivery.getAlertId()).orElse(null);
        EmailMessage message = EmailMessage.builder()
                .to(delivery.getDestination())
                .subject(buildSubject(alert))
                .body(buildBody(alert))
                .build();
        return emailSenderPort.send(message);
    }

    private void handleFailure(
            AlertDeliveryRecord delivery,
            int attempt,
            DeliveryFailureType failureType,
            String message,
            Instant now,
            Exception ex) {
        boolean terminal = failureType == DeliveryFailureType.NON_RETRYABLE || attempt >= properties.getMaxAttempts();
        delivery.setAttemptCount(attempt);
        delivery.setLastError(truncate(message, 2000));
        delivery.setUpdatedAt(now);

        if (terminal) {
            delivery.setStatus(AlertDeliveryStatus.FAILED);
            delivery.setNextAttemptAt(null);
            alertDeliveryRepository.save(delivery);
            dlqPublisher.publishFailure(delivery, failureType, truncate(message, 2000));
            log.error(
                    "Delivery permanently failed for deliveryId={} alertId={} channel={} attempt={} failureType={}",
                    delivery.getId(),
                    delivery.getAlertId(),
                    delivery.getChannel(),
                    attempt,
                    failureType,
                    ex);
            return;
        }

        long backoffSeconds = computeBackoffSeconds(attempt);
        delivery.setStatus(AlertDeliveryStatus.RETRY_SCHEDULED);
        delivery.setNextAttemptAt(now.plusSeconds(backoffSeconds));
        alertDeliveryRepository.save(delivery);
        log.warn(
                "Delivery retry scheduled for deliveryId={} alertId={} channel={} attempt={} nextAttemptAt={}",
                delivery.getId(),
                delivery.getAlertId(),
                delivery.getChannel(),
                attempt,
                delivery.getNextAttemptAt(),
                ex);
    }

    private long computeBackoffSeconds(int attempt) {
        long base = Math.max(1L, properties.getRetryBaseSeconds());
        long max = Math.max(base, properties.getRetryMaxSeconds());
        int exponent = Math.max(0, attempt - 1);
        long multiplier = 1L << Math.min(exponent, 20);
        long value = base * multiplier;
        return Math.min(value, max);
    }

    private int normalizeAttempts(AlertDeliveryRecord delivery) {
        if (delivery.getAttemptCount() == null || delivery.getAttemptCount() < 0) {
            return 0;
        }
        return delivery.getAttemptCount();
    }

    private String buildSubject(Alert alert) {
        if (alert != null && alert.getHeadline() != null && !alert.getHeadline().isBlank()) {
            return "[Weather Alert] " + alert.getHeadline();
        }
        if (alert != null && alert.getEventType() != null && !alert.getEventType().isBlank()) {
            return "[Weather Alert] " + alert.getEventType();
        }
        return "[Weather Alert] New alert triggered";
    }

    private String buildBody(Alert alert) {
        if (alert == null) {
            return "A weather alert has been triggered.";
        }
        StringBuilder body = new StringBuilder("A weather alert has been triggered.");
        if (alert.getLocation() != null && !alert.getLocation().isBlank()) {
            body.append("\n\nLocation: ").append(alert.getLocation());
        }
        if (alert.getSeverity() != null && !alert.getSeverity().isBlank()) {
            body.append("\nSeverity: ").append(alert.getSeverity());
        }
        if (alert.getReason() != null && !alert.getReason().isBlank()) {
            body.append("\nReason: ").append(alert.getReason());
        }
        if (alert.getDescription() != null && !alert.getDescription().isBlank()) {
            body.append("\n\nDetails:\n").append(alert.getDescription());
        }
        return body.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
