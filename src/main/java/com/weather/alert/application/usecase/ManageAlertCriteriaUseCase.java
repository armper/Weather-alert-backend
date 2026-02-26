package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
import com.weather.alert.application.exception.CriteriaNotFoundException;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.EmailSenderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import com.weather.alert.domain.service.AlertProcessingService;
import com.weather.alert.domain.service.notification.EmailDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Use case for managing alert criteria (Command)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManageAlertCriteriaUseCase {
    
    private final AlertCriteriaRepositoryPort criteriaRepository;
    private final AlertProcessingService alertProcessingService;
    private final UserRepositoryPort userRepository;
    private final EmailSenderPort emailSenderPort;

    @Value("${app.notification.criteria-created.send-email:false}")
    private boolean sendCriteriaCreatedEmail;

    @Value("${app.notification.criteria-created.email-subject:Weather Alert criteria created}")
    private String criteriaCreatedEmailSubject;

    @Value("${app.notification.criteria-deleted.send-email:false}")
    private boolean sendCriteriaDeletedEmail;

    @Value("${app.notification.criteria-deleted.email-subject:Weather Alert criteria deleted}")
    private String criteriaDeletedEmailSubject;
    
    public AlertCriteria createCriteria(CreateAlertCriteriaRequest request) {
        AlertCriteria criteria = AlertCriteria.builder()
                .id(UUID.randomUUID().toString())
                .userId(request.getUserId())
                .name(normalizeName(request.getName()))
                .location(request.getLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .radiusKm(request.getRadiusKm())
                .eventType(request.getEventType())
                .minSeverity(request.getMinSeverity())
                .maxTemperature(request.getMaxTemperature())
                .minTemperature(request.getMinTemperature())
                .maxWindSpeed(request.getMaxWindSpeed())
                .maxPrecipitation(request.getMaxPrecipitation())
                .temperatureThreshold(request.getTemperatureThreshold())
                .temperatureDirection(request.getTemperatureDirection())
                .rainThreshold(request.getRainThreshold())
                .rainThresholdType(request.getRainThresholdType())
                .monitorCurrent(defaultMonitorCurrent(request.getMonitorCurrent()))
                .monitorForecast(defaultMonitorForecast(request.getMonitorForecast()))
                .forecastWindowHours(defaultForecastWindowHours(request.getForecastWindowHours()))
                .temperatureUnit(defaultTemperatureUnit(request.getTemperatureUnit()))
                .oncePerEvent(defaultOncePerEvent(request.getOncePerEvent()))
                .rearmWindowMinutes(defaultRearmWindowMinutes(request.getRearmWindowMinutes()))
                .enabled(true)
                .build();
        
        AlertCriteria saved = criteriaRepository.save(criteria);
        try {
            alertProcessingService.processCriteriaImmediately(saved);
        } catch (Exception ex) {
            log.warn("Immediate criteria evaluation failed for criteria {}: {}", saved.getId(), ex.getMessage());
        }
        sendCriteriaCreatedEmailIfEnabled(saved);
        return saved;
    }
    
    public void deleteCriteria(String criteriaId) {
        AlertCriteria criteria = criteriaRepository.findById(criteriaId)
                .orElseThrow(() -> new CriteriaNotFoundException(criteriaId));
        criteriaRepository.delete(criteriaId);
        sendCriteriaDeletedEmailIfEnabled(criteria);
    }
    
    public AlertCriteria updateCriteria(String criteriaId, CreateAlertCriteriaRequest request) {
        return criteriaRepository.findById(criteriaId)
                .map(existing -> {
                    existing.setName(normalizeName(request.getName()));
                    existing.setLocation(request.getLocation());
                    existing.setLatitude(request.getLatitude());
                    existing.setLongitude(request.getLongitude());
                    existing.setRadiusKm(request.getRadiusKm());
                    existing.setEventType(request.getEventType());
                    existing.setMinSeverity(request.getMinSeverity());
                    existing.setMaxTemperature(request.getMaxTemperature());
                    existing.setMinTemperature(request.getMinTemperature());
                    existing.setMaxWindSpeed(request.getMaxWindSpeed());
                    existing.setMaxPrecipitation(request.getMaxPrecipitation());
                    existing.setTemperatureThreshold(request.getTemperatureThreshold());
                    existing.setTemperatureDirection(request.getTemperatureDirection());
                    existing.setRainThreshold(request.getRainThreshold());
                    existing.setRainThresholdType(request.getRainThresholdType());
                    existing.setMonitorCurrent(defaultMonitorCurrent(request.getMonitorCurrent()));
                    existing.setMonitorForecast(defaultMonitorForecast(request.getMonitorForecast()));
                    existing.setForecastWindowHours(defaultForecastWindowHours(request.getForecastWindowHours()));
                    existing.setTemperatureUnit(defaultTemperatureUnit(request.getTemperatureUnit()));
                    existing.setOncePerEvent(defaultOncePerEvent(request.getOncePerEvent()));
                    existing.setRearmWindowMinutes(defaultRearmWindowMinutes(request.getRearmWindowMinutes()));
                    return criteriaRepository.save(existing);
                })
                .orElseThrow(() -> new CriteriaNotFoundException(criteriaId));
    }

    private boolean defaultMonitorCurrent(Boolean monitorCurrent) {
        return monitorCurrent == null || monitorCurrent;
    }

    private boolean defaultMonitorForecast(Boolean monitorForecast) {
        return monitorForecast == null || monitorForecast;
    }

    private int defaultForecastWindowHours(Integer forecastWindowHours) {
        return forecastWindowHours == null ? 48 : forecastWindowHours;
    }

    private AlertCriteria.TemperatureUnit defaultTemperatureUnit(AlertCriteria.TemperatureUnit temperatureUnit) {
        return temperatureUnit == null ? AlertCriteria.TemperatureUnit.F : temperatureUnit;
    }

    private boolean defaultOncePerEvent(Boolean oncePerEvent) {
        return oncePerEvent == null || oncePerEvent;
    }

    private int defaultRearmWindowMinutes(Integer rearmWindowMinutes) {
        return rearmWindowMinutes == null ? 0 : rearmWindowMinutes;
    }

    private void sendCriteriaCreatedEmailIfEnabled(AlertCriteria criteria) {
        if (!sendCriteriaCreatedEmail || criteria == null || criteria.getUserId() == null || criteria.getUserId().isBlank()) {
            return;
        }
        userRepository.findById(criteria.getUserId()).ifPresent(user -> sendCriteriaCreatedEmail(criteria, user));
    }

    private void sendCriteriaCreatedEmail(AlertCriteria criteria, User user) {
        if (!canReceiveCriteriaNotificationEmail(user)) {
            return;
        }

        String recipient = normalizeEmail(user.getEmail());
        if (recipient == null) {
            return;
        }

        String body = """
                Hi %s,

                Your weather alert has been created successfully.

                What this alert watches:
                - Alert name: %s
                - Condition: %s
                - Area: %s

                How often we check:
                - Current conditions: %s
                - Forecast conditions: %s
                - Forecast window: %s
                - Notify once per event: %s
                - Rearm window: %s

                You will now receive notifications when this alert is triggered.

                Weather Alert
                """
                .formatted(
                        displayName(user),
                        displayAlertName(criteria),
                        describeAlertSummary(criteria),
                        describeArea(criteria),
                        enabledText(criteria.getMonitorCurrent()),
                        enabledText(criteria.getMonitorForecast()),
                        describeForecastWindow(criteria.getForecastWindowHours()),
                        enabledText(criteria.getOncePerEvent()),
                        describeRearmWindow(criteria.getRearmWindowMinutes()));
        try {
            emailSenderPort.send(EmailMessage.builder()
                    .to(recipient)
                    .subject(criteriaCreatedEmailSubject)
                    .body(body)
                    .build());
        } catch (EmailDeliveryException ex) {
            log.warn(
                    "Criteria-created email delivery failed for criteria {} (user={}): {}",
                    criteria.getId(),
                    criteria.getUserId(),
                    ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn(
                    "Unexpected failure sending criteria-created email for criteria {} (user={}): {}",
                    criteria.getId(),
                    criteria.getUserId(),
                    ex.getMessage());
        }
    }

    private void sendCriteriaDeletedEmailIfEnabled(AlertCriteria criteria) {
        if (!sendCriteriaDeletedEmail || criteria == null || criteria.getUserId() == null || criteria.getUserId().isBlank()) {
            return;
        }
        userRepository.findById(criteria.getUserId()).ifPresent(user -> sendCriteriaDeletedEmail(criteria, user));
    }

    private void sendCriteriaDeletedEmail(AlertCriteria criteria, User user) {
        if (!canReceiveCriteriaNotificationEmail(user)) {
            return;
        }

        String recipient = normalizeEmail(user.getEmail());
        if (recipient == null) {
            return;
        }

        String body = """
                Hi %s,

                Your weather alert has been deleted.

                Removed alert:
                - Alert name: %s
                - Condition: %s
                - Area: %s

                You will no longer receive notifications from this alert.
                You can create a new alert at any time.

                Weather Alert
                """
                .formatted(
                        displayName(user),
                        displayAlertName(criteria),
                        describeAlertSummary(criteria),
                        describeArea(criteria));
        try {
            emailSenderPort.send(EmailMessage.builder()
                    .to(recipient)
                    .subject(criteriaDeletedEmailSubject)
                    .body(body)
                    .build());
        } catch (EmailDeliveryException ex) {
            log.warn(
                    "Criteria-deleted email delivery failed for criteria {} (user={}): {}",
                    criteria.getId(),
                    criteria.getUserId(),
                    ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn(
                    "Unexpected failure sending criteria-deleted email for criteria {} (user={}): {}",
                    criteria.getId(),
                    criteria.getUserId(),
                    ex.getMessage());
        }
    }

    private boolean canReceiveCriteriaNotificationEmail(User user) {
        if (user == null) {
            return false;
        }
        return normalizeEmail(user.getEmail()) != null
                && Boolean.TRUE.equals(user.getEmailVerified())
                && Boolean.TRUE.equals(user.getEmailEnabled());
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String displayName(User user) {
        if (user == null || user.getName() == null || user.getName().isBlank()) {
            return "there";
        }
        return user.getName().trim();
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String displayAlertName(AlertCriteria criteria) {
        if (criteria != null && criteria.getName() != null && !criteria.getName().isBlank()) {
            return criteria.getName().trim();
        }
        return "Custom weather alert";
    }

    private String enabledText(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? "On" : "Off";
    }

    private String describeForecastWindow(Integer hours) {
        if (hours == null || hours <= 0) {
            return "-";
        }
        return hours + " hours";
    }

    private String describeRearmWindow(Integer minutes) {
        if (minutes == null) {
            return "-";
        }
        if (minutes == 0) {
            return "None";
        }
        return minutes + " minutes";
    }

    private String formatNumber(Double value) {
        if (value == null) {
            return "-";
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
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

    private String describeArea(AlertCriteria criteria) {
        if (criteria == null) {
            return "your selected area";
        }
        if (criteria.getLocation() != null && !criteria.getLocation().isBlank()) {
            return criteria.getLocation().trim();
        }
        if (criteria.getLatitude() != null && criteria.getLongitude() != null) {
            return formatNumber(criteria.getLatitude()) + ", " + formatNumber(criteria.getLongitude());
        }
        return "your selected area";
    }
}
