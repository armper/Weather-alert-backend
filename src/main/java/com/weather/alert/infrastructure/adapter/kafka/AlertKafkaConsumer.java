package com.weather.alert.infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.AlertRepositoryPort;
import com.weather.alert.domain.port.EmailSenderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import com.weather.alert.domain.service.notification.EmailDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Kafka consumer for processing alerts
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertKafkaConsumer {
    
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AlertRepositoryPort alertRepository;
    private final UserRepositoryPort userRepository;
    private final EmailSenderPort emailSenderPort;
    
    @KafkaListener(topics = "weather-alerts", groupId = "alert-processor")
    public void consumeAlert(String message) {
        try {
            Alert alert = objectMapper.readValue(message, Alert.class);
            log.info("Consumed alert {} for user {}", alert.getId(), alert.getUserId());
            
            // Process the alert - send notifications via email, SMS, push, etc.
            processAlert(alert);
            if (alert.getId() != null && !alert.getId().isBlank()) {
                alertRepository.markAsSent(alert.getId(), Instant.now());
            }
            if (alert.getUserId() != null && !alert.getUserId().isBlank()) {
                simpMessagingTemplate.convertAndSend("/topic/alerts/" + alert.getUserId(), alert);
            }
        } catch (Exception e) {
            log.error("Error consuming alert message", e);
        }
    }
    
    private void processAlert(Alert alert) {
        if (alert == null || alert.getUserId() == null || alert.getUserId().isBlank()) {
            return;
        }
        User user = userRepository.findById(alert.getUserId()).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.info("Skipping email delivery for alert {}: user email not configured", alert.getId());
            return;
        }

        EmailMessage message = EmailMessage.builder()
                .to(user.getEmail())
                .subject(buildSubject(alert))
                .body(buildBody(alert))
                .build();
        try {
            String providerMessageId = emailSenderPort.send(message).providerMessageId();
            log.info(
                    "Delivered email for alert {} to {} (providerMessageId={})",
                    alert.getId(),
                    user.getEmail(),
                    providerMessageId == null ? "n/a" : providerMessageId);
        } catch (EmailDeliveryException ex) {
            log.error(
                    "Email delivery failed for alert {} (failureType={}): {}",
                    alert.getId(),
                    ex.getFailureType(),
                    ex.getMessage(),
                    ex);
        }
    }

    private String buildSubject(Alert alert) {
        String headline = alert.getHeadline();
        if (headline != null && !headline.isBlank()) {
            return "[Weather Alert] " + headline;
        }
        if (alert.getEventType() != null && !alert.getEventType().isBlank()) {
            return "[Weather Alert] " + alert.getEventType();
        }
        return "[Weather Alert] New alert triggered";
    }

    private String buildBody(Alert alert) {
        StringBuilder body = new StringBuilder();
        body.append("A weather alert has been triggered.");
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
}
