package com.weather.alert.application.usecase;

import com.weather.alert.application.service.BillingPlanService;
import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.AlertDeliveryStatus;
import com.weather.alert.domain.model.BillingEntitlements;
import com.weather.alert.domain.model.BillingPlan;
import com.weather.alert.domain.model.DeliveryFailureType;
import com.weather.alert.domain.model.EmailMessage;
import com.weather.alert.domain.model.EmailSendResult;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.SmsMessage;
import com.weather.alert.domain.model.SmsSendResult;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.AlertDeliveryDlqPublisherPort;
import com.weather.alert.domain.port.AlertDeliveryRepositoryPort;
import com.weather.alert.domain.port.AlertRepositoryPort;
import com.weather.alert.domain.port.EmailSenderPort;
import com.weather.alert.domain.port.SmsSenderPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import com.weather.alert.domain.port.WeatherDataPort;
import com.weather.alert.domain.service.notification.EmailDeliveryException;
import com.weather.alert.infrastructure.config.NotificationDeliveryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessAlertDeliveryTaskUseCaseTest {

    @Mock
    private AlertDeliveryRepositoryPort alertDeliveryRepository;

    @Mock
    private AlertRepositoryPort alertRepository;

    @Mock
    private AlertCriteriaRepositoryPort alertCriteriaRepository;

    @Mock
    private EmailSenderPort emailSenderPort;

    @Mock
    private SmsSenderPort smsSenderPort;

    @Mock
    private AlertDeliveryDlqPublisherPort dlqPublisher;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private BillingPlanService billingPlanService;

    @Mock
    private WeatherDataPort weatherDataPort;

    private ProcessAlertDeliveryTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        NotificationDeliveryProperties properties = new NotificationDeliveryProperties();
        properties.setMaxAttempts(3);
        properties.setRetryBaseSeconds(10);
        properties.setRetryMaxSeconds(60);
        useCase = new ProcessAlertDeliveryTaskUseCase(
                alertDeliveryRepository,
                alertRepository,
                alertCriteriaRepository,
                emailSenderPort,
                smsSenderPort,
                dlqPublisher,
                properties,
                userRepository,
                billingPlanService,
                weatherDataPort);
        ReflectionTestUtils.setField(useCase, "frontendBaseUrl", "https://skypandaweather.com");
        lenient().when(userRepository.findById("dev-admin")).thenReturn(Optional.of(User.builder()
                .id("dev-admin")
                .build()));
        lenient().when(billingPlanService.resolveEntitlements(any(User.class))).thenReturn(BillingEntitlements.builder()
                .plan(BillingPlan.PRO)
                .paidPlan(true)
                .maxActiveAlerts(50)
                .adSponsoredEmails(false)
                .build());
        lenient().when(weatherDataPort.fetchAlertsForLocation(anyDouble(), anyDouble())).thenReturn(List.of());
    }

    @Test
    void shouldMarkSentWhenEmailDeliverySucceeds() {
        AlertDeliveryRecord delivery = pending("delivery-1", 0);
        when(alertDeliveryRepository.findById("delivery-1")).thenReturn(Optional.of(delivery));
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(Alert.builder()
                .id("alert-1")
                .criteriaId("criteria-1")
                .location("Orlando")
                .conditionSource("CURRENT")
                .conditionTemperatureC(24.4)
                .reason("Matched CURRENT: Partly Cloudy")
                .description("Latest NOAA observation from station KORL")
                .alertTime(Instant.parse("2026-02-26T20:41:23.668488Z"))
                .build()));
        when(alertCriteriaRepository.findById("criteria-1")).thenReturn(Optional.of(AlertCriteria.builder()
                .id("criteria-1")
                .name("Bring a jacket")
                .location("Orlando")
                .temperatureThreshold(80.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .oncePerEvent(true)
                .build()));
        when(emailSenderPort.send(any())).thenReturn(new EmailSendResult("provider-id-1"));

        useCase.processTask("delivery-1");

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSenderPort).send(emailCaptor.capture());
        assertEquals("SkyPanda Alert: Bring a jacket", emailCaptor.getValue().subject());
        assertTrue(emailCaptor.getValue().body().contains("Alert name: Bring a jacket"));
        assertTrue(emailCaptor.getValue().body().contains("Area: Orlando"));
        assertTrue(emailCaptor.getValue().body().contains("Rule: temperature is below 80 F"));
        assertTrue(emailCaptor.getValue().body().contains("Matched reading: temperature 75.9 F"));
        assertTrue(emailCaptor.getValue().body().contains("Source: Current conditions"));
        assertTrue(emailCaptor.getValue().body().contains("Current conditions: Partly Cloudy"));
        assertTrue(emailCaptor.getValue().body().contains("https://skypandaweather.com/app/events"));
        assertTrue(emailCaptor.getValue().body().contains("SkyPanda Alerts"));

        ArgumentCaptor<AlertDeliveryRecord> captor = ArgumentCaptor.forClass(AlertDeliveryRecord.class);
        verify(alertDeliveryRepository, atLeast(2)).save(captor.capture());
        AlertDeliveryRecord finalState = captor.getValue();
        assertEquals(AlertDeliveryStatus.SENT, finalState.getStatus());
        assertEquals(1, finalState.getAttemptCount());
        assertEquals("provider-id-1", finalState.getProviderMessageId());
        assertNotNull(finalState.getSentAt());
        verify(alertRepository).markAsSent("alert-1", finalState.getSentAt());
        verify(dlqPublisher, never()).publishFailure(any(), any(), any());
    }

    @Test
    void shouldNotIncludeTemperatureMatchedReadingForWindOnlyAlert() {
        AlertDeliveryRecord delivery = pending("delivery-2", 0);
        when(alertDeliveryRepository.findById("delivery-2")).thenReturn(Optional.of(delivery));
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(Alert.builder()
                .id("alert-1")
                .criteriaId("criteria-wind")
                .location("Orlando")
                .conditionSource("CURRENT")
                .conditionTemperatureC(27.0)
                .reason("Matched CURRENT: Mostly Cloudy")
                .description("Latest NOAA observation from station KORL")
                .alertTime(Instant.parse("2026-02-27T20:04:58.859797Z"))
                .build()));
        when(alertCriteriaRepository.findById("criteria-wind")).thenReturn(Optional.of(AlertCriteria.builder()
                .id("criteria-wind")
                .name("Disastrous Winds")
                .location("Orlando")
                .maxWindSpeed(70.0)
                .oncePerEvent(true)
                .build()));
        when(emailSenderPort.send(any())).thenReturn(new EmailSendResult("provider-id-2"));

        useCase.processTask("delivery-2");

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSenderPort).send(emailCaptor.capture());
        String body = emailCaptor.getValue().body();
        assertTrue(body.contains("Rule: wind speed is above 70 km/h"));
        assertFalse(body.contains("Matched reading: temperature"));
    }

    @Test
    void shouldAppendSponsoredFooterForFreePlanEmail() {
        AlertDeliveryRecord delivery = pending("delivery-ads", 0);
        when(alertDeliveryRepository.findById("delivery-ads")).thenReturn(Optional.of(delivery));
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(Alert.builder()
                .id("alert-1")
                .userId("dev-admin")
                .criteriaId("criteria-1")
                .location("Orlando")
                .alertTime(Instant.parse("2026-03-10T03:00:00Z"))
                .build()));
        when(alertCriteriaRepository.findById("criteria-1")).thenReturn(Optional.of(AlertCriteria.builder()
                .id("criteria-1")
                .name("Single Free Alert")
                .location("Orlando")
                .temperatureThreshold(80.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .build()));
        when(billingPlanService.resolveEntitlements(any(User.class))).thenReturn(BillingEntitlements.builder()
                .plan(BillingPlan.FREE)
                .paidPlan(false)
                .maxActiveAlerts(1)
                .adSponsoredEmails(true)
                .build());
        when(emailSenderPort.send(any())).thenReturn(new EmailSendResult("provider-id-ads"));

        useCase.processTask("delivery-ads");

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSenderPort).send(emailCaptor.capture());
        assertTrue(emailCaptor.getValue().body().contains("Sponsored message:"));
        assertTrue(emailCaptor.getValue().body().contains("Upgrade to SkyPanda Family Plan"));
    }

    @Test
    void shouldIncludeOfficialNoaaAlertsInEmailBody() {
        AlertDeliveryRecord delivery = pending("delivery-noaa", 0);
        when(alertDeliveryRepository.findById("delivery-noaa")).thenReturn(Optional.of(delivery));
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(Alert.builder()
                .id("alert-1")
                .criteriaId("criteria-1")
                .location("Orlando")
                .conditionSource("CURRENT")
                .alertTime(Instant.parse("2026-03-10T03:00:00Z"))
                .build()));
        when(alertCriteriaRepository.findById("criteria-1")).thenReturn(Optional.of(AlertCriteria.builder()
                .id("criteria-1")
                .name("Storm watch")
                .location("Orlando")
                .latitude(28.5383)
                .longitude(-81.3792)
                .temperatureThreshold(80.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .build()));
        when(weatherDataPort.fetchAlertsForLocation(28.5383, -81.3792)).thenReturn(List.of(
                WeatherData.builder()
                        .id("noaa-1")
                        .eventType("Severe Thunderstorm Warning")
                        .severity("SEVERE")
                        .location("Orange County")
                        .expires(Instant.parse("2026-03-10T04:30:00Z"))
                        .build()));
        when(emailSenderPort.send(any())).thenReturn(new EmailSendResult("provider-id-noaa"));

        useCase.processTask("delivery-noaa");

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSenderPort).send(emailCaptor.capture());
        assertTrue(emailCaptor.getValue().body().contains("Official NOAA alerts in this area:"));
        assertTrue(emailCaptor.getValue().body().contains("Severe Thunderstorm Warning | severity severe | until 2026-03-10T04:30:00Z | for Orange County"));
    }

    @Test
    void shouldMarkSentWhenSmsDeliverySucceeds() {
        AlertDeliveryRecord delivery = AlertDeliveryRecord.builder()
                .id("delivery-sms")
                .alertId("alert-1")
                .userId("dev-admin")
                .channel(NotificationChannel.SMS)
                .destination("+14075550199")
                .status(AlertDeliveryStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(Instant.now().minusSeconds(1))
                .createdAt(Instant.now().minusSeconds(60))
                .updatedAt(Instant.now().minusSeconds(60))
                .build();
        when(alertDeliveryRepository.findById("delivery-sms")).thenReturn(Optional.of(delivery));
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(Alert.builder()
                .id("alert-1")
                .criteriaId("criteria-1")
                .location("Orlando")
                .conditionSource("CURRENT")
                .conditionTemperatureC(24.4)
                .reason("Matched CURRENT: Partly Cloudy")
                .alertTime(Instant.parse("2026-02-26T20:41:23.668488Z"))
                .build()));
        when(alertCriteriaRepository.findById("criteria-1")).thenReturn(Optional.of(AlertCriteria.builder()
                .id("criteria-1")
                .name("Bring a jacket")
                .location("Orlando")
                .temperatureThreshold(80.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .build()));
        when(smsSenderPort.send(any())).thenReturn(new SmsSendResult("sms-provider-id-1"));

        useCase.processTask("delivery-sms");

        ArgumentCaptor<SmsMessage> smsCaptor = ArgumentCaptor.forClass(SmsMessage.class);
        verify(smsSenderPort).send(smsCaptor.capture());
        assertEquals("+14075550199", smsCaptor.getValue().to());
        assertTrue(smsCaptor.getValue().body().contains("Bring a jacket"));
        assertTrue(smsCaptor.getValue().body().contains("Orlando"));

        ArgumentCaptor<AlertDeliveryRecord> captor = ArgumentCaptor.forClass(AlertDeliveryRecord.class);
        verify(alertDeliveryRepository, atLeast(2)).save(captor.capture());
        AlertDeliveryRecord finalState = captor.getValue();
        assertEquals(AlertDeliveryStatus.SENT, finalState.getStatus());
        assertEquals("sms-provider-id-1", finalState.getProviderMessageId());
    }

    @Test
    void shouldScheduleRetryForRetryableFailure() {
        AlertDeliveryRecord delivery = pending("delivery-1", 0);
        when(alertDeliveryRepository.findById("delivery-1")).thenReturn(Optional.of(delivery));
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(Alert.builder().id("alert-1").build()));
        when(emailSenderPort.send(any())).thenThrow(new EmailDeliveryException(
                DeliveryFailureType.RETRYABLE,
                "smtp timeout",
                null));

        useCase.processTask("delivery-1");

        ArgumentCaptor<AlertDeliveryRecord> captor = ArgumentCaptor.forClass(AlertDeliveryRecord.class);
        verify(alertDeliveryRepository, atLeast(2)).save(captor.capture());
        AlertDeliveryRecord finalState = captor.getValue();
        assertEquals(AlertDeliveryStatus.RETRY_SCHEDULED, finalState.getStatus());
        assertEquals(1, finalState.getAttemptCount());
        assertNotNull(finalState.getNextAttemptAt());
        assertTrue(finalState.getNextAttemptAt().isAfter(Instant.now().minusSeconds(1)));
        verify(dlqPublisher, never()).publishFailure(any(), any(), any());
    }

    @Test
    void shouldMarkFailedAndPublishDlqForNonRetryableFailure() {
        AlertDeliveryRecord delivery = pending("delivery-1", 0);
        when(alertDeliveryRepository.findById("delivery-1")).thenReturn(Optional.of(delivery));
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(Alert.builder().id("alert-1").build()));
        when(emailSenderPort.send(any())).thenThrow(new EmailDeliveryException(
                DeliveryFailureType.NON_RETRYABLE,
                "invalid email",
                null));

        useCase.processTask("delivery-1");

        ArgumentCaptor<AlertDeliveryRecord> captor = ArgumentCaptor.forClass(AlertDeliveryRecord.class);
        verify(alertDeliveryRepository, atLeast(2)).save(captor.capture());
        AlertDeliveryRecord finalState = captor.getValue();
        assertEquals(AlertDeliveryStatus.FAILED, finalState.getStatus());
        assertEquals(1, finalState.getAttemptCount());
        verify(dlqPublisher).publishFailure(any(AlertDeliveryRecord.class), any(), any());
    }

    @Test
    void shouldMarkFailedWhenMaxAttemptsReached() {
        AlertDeliveryRecord delivery = pending("delivery-1", 2);
        when(alertDeliveryRepository.findById("delivery-1")).thenReturn(Optional.of(delivery));
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(Alert.builder().id("alert-1").build()));
        when(emailSenderPort.send(any())).thenThrow(new EmailDeliveryException(
                DeliveryFailureType.RETRYABLE,
                "smtp timeout",
                null));

        useCase.processTask("delivery-1");

        ArgumentCaptor<AlertDeliveryRecord> captor = ArgumentCaptor.forClass(AlertDeliveryRecord.class);
        verify(alertDeliveryRepository, atLeast(2)).save(captor.capture());
        AlertDeliveryRecord finalState = captor.getValue();
        assertEquals(AlertDeliveryStatus.FAILED, finalState.getStatus());
        assertEquals(3, finalState.getAttemptCount());
        verify(dlqPublisher).publishFailure(any(AlertDeliveryRecord.class), any(), any());
    }

    private AlertDeliveryRecord pending(String id, int attempts) {
        return AlertDeliveryRecord.builder()
                .id(id)
                .alertId("alert-1")
                .userId("dev-admin")
                .channel(NotificationChannel.EMAIL)
                .destination("dev-admin@example.com")
                .status(AlertDeliveryStatus.PENDING)
                .attemptCount(attempts)
                .nextAttemptAt(Instant.now().minusSeconds(1))
                .createdAt(Instant.now().minusSeconds(60))
                .updatedAt(Instant.now().minusSeconds(60))
                .build();
    }
}
