package com.weather.alert.application.usecase;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.AlertDeliveryStatus;
import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.EmailSendResult;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessAlertDeliveryTaskUseCase {

    private final AlertDeliveryRepositoryPort alertDeliveryRepository;
    private final AlertRepositoryPort alertRepository;
    private final AlertCriteriaRepositoryPort alertCriteriaRepository;
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
        AlertCriteria criteria = findCriteria(alert);
        EmailMessage message = EmailMessage.builder()
                .to(delivery.getDestination())
                .subject(buildSubject(alert, criteria))
                .body(buildBody(alert, criteria))
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

    private AlertCriteria findCriteria(Alert alert) {
        if (alert == null || alert.getCriteriaId() == null || alert.getCriteriaId().isBlank()) {
            return null;
        }
        return alertCriteriaRepository.findById(alert.getCriteriaId()).orElse(null);
    }

    private String buildSubject(Alert alert, AlertCriteria criteria) {
        String alertName = displayAlertName(criteria, alert);
        if (alertName != null) {
            return "Weather Alert: " + alertName;
        }
        if (alert != null && alert.getHeadline() != null && !alert.getHeadline().isBlank()) {
            return "Weather Alert: " + alert.getHeadline();
        }
        if (alert != null && alert.getEventType() != null && !alert.getEventType().isBlank()) {
            return "Weather Alert: " + alert.getEventType();
        }
        return "Weather Alert: New alert triggered";
    }

    private String buildBody(Alert alert, AlertCriteria criteria) {
        if (alert == null) {
            return """
                    Hi there,

                    A weather alert has been triggered.

                    Please check your Weather Alert app for details.

                    Weather Alert
                    """;
        }
        StringBuilder body = new StringBuilder();
        body.append("Hi there,\n\n");
        body.append("Your weather alert was triggered.\n\n");
        body.append("Alert name: ").append(displayAlertName(criteria, alert)).append('\n');
        body.append("Area: ").append(describeArea(alert, criteria)).append('\n');
        body.append("Rule: ").append(describeAlertSummary(criteria)).append('\n');

        String matchedReading = describeMatchedReading(alert, criteria);
        if (matchedReading != null) {
            body.append("Matched reading: ").append(matchedReading).append('\n');
        }

        String conditionSource = describeConditionSource(alert);
        if (conditionSource != null) {
            body.append("Source: ").append(conditionSource).append('\n');
        }

        if (alert.getAlertTime() != null) {
            body.append("Triggered at: ").append(alert.getAlertTime()).append('\n');
        }

        String humanizedReason = humanizeReason(alert.getReason());
        if (humanizedReason != null) {
            body.append("\nWhy you received this:\n");
            body.append(humanizedReason).append('\n');
        }

        if (alert.getDescription() != null && !alert.getDescription().isBlank()) {
            body.append("\nAdditional details:\n");
            body.append(alert.getDescription().trim()).append('\n');
        }

        if (criteria != null && Boolean.TRUE.equals(criteria.getOncePerEvent())) {
            body.append("\nYou will get one notification per matching event.\n");
        }

        body.append("\nYou can review or update this alert anytime in the app.\n");
        body.append("\nWeather Alert");
        return body.toString();
    }

    private String displayAlertName(AlertCriteria criteria, Alert alert) {
        if (criteria != null && criteria.getName() != null && !criteria.getName().isBlank()) {
            return criteria.getName().trim();
        }
        if (alert != null && alert.getHeadline() != null && !alert.getHeadline().isBlank()) {
            return alert.getHeadline().trim();
        }
        if (alert != null && alert.getEventType() != null && !alert.getEventType().isBlank()) {
            return alert.getEventType().trim();
        }
        return "Custom weather alert";
    }

    private String describeArea(Alert alert, AlertCriteria criteria) {
        if (criteria != null && criteria.getLocation() != null && !criteria.getLocation().isBlank()) {
            return criteria.getLocation().trim();
        }
        if (alert != null && alert.getLocation() != null && !alert.getLocation().isBlank()) {
            return alert.getLocation().trim();
        }
        if (criteria != null && criteria.getLatitude() != null && criteria.getLongitude() != null) {
            return formatNumber(criteria.getLatitude()) + ", " + formatNumber(criteria.getLongitude());
        }
        return "your selected area";
    }

    private String describeAlertSummary(AlertCriteria criteria) {
        if (criteria == null) {
            return "your custom weather condition is met";
        }

        List<String> conditions = new ArrayList<>();
        if (criteria.getTemperatureThreshold() != null && criteria.getTemperatureDirection() != null) {
            String direction = criteria.getTemperatureDirection() == AlertCriteria.TemperatureDirection.BELOW
                    ? "below"
                    : "above";
            String unit = criteria.getTemperatureUnit() == null ? "F" : criteria.getTemperatureUnit().name();
            conditions.add("temperature is %s %s %s"
                    .formatted(direction, formatNumber(criteria.getTemperatureThreshold()), unit));
        }
        if (criteria.getMinTemperature() != null || criteria.getMaxTemperature() != null) {
            conditions.add("temperature enters your configured range");
        }
        if (criteria.getMaxWindSpeed() != null) {
            conditions.add("wind speed is above %s km/h".formatted(formatNumber(criteria.getMaxWindSpeed())));
        }
        if (criteria.getRainThreshold() != null && criteria.getRainThresholdType() != null) {
            if (criteria.getRainThresholdType() == AlertCriteria.RainThresholdType.PROBABILITY) {
                conditions.add("rain chance is at or above %s%%".formatted(formatNumber(criteria.getRainThreshold())));
            } else {
                conditions.add("rain amount is at or above %s mm".formatted(formatNumber(criteria.getRainThreshold())));
            }
        }
        if (criteria.getMaxPrecipitation() != null) {
            conditions.add("rain amount is above %s mm/h".formatted(formatNumber(criteria.getMaxPrecipitation())));
        }

        if (conditions.isEmpty()) {
            return "your custom weather condition is met";
        }
        if (conditions.size() == 1) {
            return conditions.get(0);
        }
        if (conditions.size() == 2) {
            return conditions.get(0) + " and " + conditions.get(1);
        }
        return String.join("; ", conditions);
    }

    private String describeMatchedReading(Alert alert, AlertCriteria criteria) {
        if (alert == null) {
            return null;
        }

        List<String> readings = new ArrayList<>();
        boolean includeTemperature = criteria == null || hasTemperatureRule(criteria);
        if (includeTemperature && alert.getConditionTemperatureC() != null) {
            String unit = criteria != null && criteria.getTemperatureUnit() == AlertCriteria.TemperatureUnit.C ? "C" : "F";
            double value = unit.equals("C") ? alert.getConditionTemperatureC() : celsiusToFahrenheit(alert.getConditionTemperatureC());
            readings.add("temperature " + formatNumberRounded(value) + " " + unit);
        }

        boolean includeRainProbability = criteria == null || usesRainProbabilityRule(criteria);
        if (includeRainProbability && alert.getConditionPrecipitationProbability() != null) {
            readings.add("rain chance " + formatNumberRounded(alert.getConditionPrecipitationProbability()) + "%");
        }

        boolean includeRainAmount = criteria == null || usesRainAmountRule(criteria);
        if (includeRainAmount && alert.getConditionPrecipitationAmount() != null) {
            readings.add("rain amount " + formatNumberRounded(alert.getConditionPrecipitationAmount()) + " mm");
        }

        if (readings.isEmpty()) {
            return null;
        }
        if (readings.size() == 1) {
            return readings.get(0);
        }
        return String.join(", ", readings);
    }

    private boolean hasTemperatureRule(AlertCriteria criteria) {
        if (criteria == null) {
            return false;
        }
        boolean thresholdMode = criteria.getTemperatureThreshold() != null && criteria.getTemperatureDirection() != null;
        boolean legacyMode = criteria.getMinTemperature() != null || criteria.getMaxTemperature() != null;
        return thresholdMode || legacyMode;
    }

    private boolean usesRainProbabilityRule(AlertCriteria criteria) {
        if (criteria == null) {
            return false;
        }
        return criteria.getRainThreshold() != null
                && criteria.getRainThresholdType() == AlertCriteria.RainThresholdType.PROBABILITY;
    }

    private boolean usesRainAmountRule(AlertCriteria criteria) {
        if (criteria == null) {
            return false;
        }
        return (criteria.getRainThreshold() != null
                && criteria.getRainThresholdType() == AlertCriteria.RainThresholdType.AMOUNT)
                || criteria.getMaxPrecipitation() != null;
    }

    private String describeConditionSource(Alert alert) {
        if (alert == null || alert.getConditionSource() == null || alert.getConditionSource().isBlank()) {
            return null;
        }
        String source = alert.getConditionSource().trim();
        return switch (source.toUpperCase()) {
            case "CURRENT" -> "Current conditions";
            case "FORECAST" -> "Forecast conditions";
            case "ALERT" -> "Weather alerts";
            default -> source;
        };
    }

    private String humanizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String value = reason.trim();
        if (value.startsWith("Matched ")) {
            value = value.substring("Matched ".length());
        }
        value = value.replace("CURRENT:", "Current conditions:");
        value = value.replace("FORECAST:", "Forecast conditions:");
        value = value.replace("ALERT:", "Weather alerts:");
        return value;
    }

    private String formatNumber(Double value) {
        if (value == null) {
            return "-";
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private String formatNumberRounded(Double value) {
        if (value == null) {
            return "-";
        }
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private double celsiusToFahrenheit(double value) {
        return value * 9.0 / 5.0 + 32.0;
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
