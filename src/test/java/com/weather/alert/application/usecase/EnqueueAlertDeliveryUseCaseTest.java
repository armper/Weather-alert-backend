package com.weather.alert.application.usecase;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertDeliveryRecord;
import com.weather.alert.domain.model.AlertDeliveryStatus;
import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.ResolvedNotificationPreference;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.AlertDeliveryRepositoryPort;
import com.weather.alert.domain.port.AlertDeliveryTaskPublisherPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import com.weather.alert.domain.service.notification.NotificationPreferenceResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnqueueAlertDeliveryUseCaseTest {

    @Mock
    private AlertDeliveryRepositoryPort alertDeliveryRepository;

    @Mock
    private NotificationPreferenceResolverService notificationPreferenceResolverService;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AlertDeliveryTaskPublisherPort taskPublisher;

    private EnqueueAlertDeliveryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EnqueueAlertDeliveryUseCase(
                alertDeliveryRepository,
                notificationPreferenceResolverService,
                userRepository,
                taskPublisher);
    }

    @Test
    void shouldCreateDeliveryRecordAndPublishTask() {
        Alert alert = Alert.builder()
                .id("alert-1")
                .userId("dev-admin")
                .criteriaId("criteria-1")
                .build();
        when(userRepository.findById("dev-admin")).thenReturn(Optional.of(User.builder()
                .id("dev-admin")
                .email("dev-admin@example.com")
                .build()));
        when(notificationPreferenceResolverService.resolve("dev-admin", "criteria-1"))
                .thenReturn(resolved(List.of(NotificationChannel.EMAIL)));
        when(alertDeliveryRepository.findByAlertIdAndChannel("alert-1", NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());
        when(alertDeliveryRepository.save(any(AlertDeliveryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.enqueue(alert);

        ArgumentCaptor<AlertDeliveryRecord> captor = ArgumentCaptor.forClass(AlertDeliveryRecord.class);
        verify(alertDeliveryRepository).save(captor.capture());
        AlertDeliveryRecord saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals("alert-1", saved.getAlertId());
        assertEquals("dev-admin@example.com", saved.getDestination());
        assertEquals(AlertDeliveryStatus.PENDING, saved.getStatus());
        verify(taskPublisher).publishTask(saved.getId());
    }

    @Test
    void shouldReuseExistingPendingDeliveryForIdempotency() {
        Alert alert = Alert.builder()
                .id("alert-1")
                .userId("dev-admin")
                .criteriaId("criteria-1")
                .build();
        when(userRepository.findById("dev-admin")).thenReturn(Optional.of(User.builder()
                .id("dev-admin")
                .email("dev-admin@example.com")
                .build()));
        when(notificationPreferenceResolverService.resolve("dev-admin", "criteria-1"))
                .thenReturn(resolved(List.of(NotificationChannel.EMAIL)));
        when(alertDeliveryRepository.findByAlertIdAndChannel("alert-1", NotificationChannel.EMAIL))
                .thenReturn(Optional.of(AlertDeliveryRecord.builder()
                        .id("delivery-existing")
                        .status(AlertDeliveryStatus.PENDING)
                        .build()));

        useCase.enqueue(alert);

        verify(alertDeliveryRepository, never()).save(any(AlertDeliveryRecord.class));
        verify(taskPublisher).publishTask("delivery-existing");
    }

    @Test
    void shouldSkipWhenDestinationMissing() {
        Alert alert = Alert.builder()
                .id("alert-1")
                .userId("dev-admin")
                .criteriaId("criteria-1")
                .build();
        when(userRepository.findById("dev-admin")).thenReturn(Optional.of(User.builder()
                .id("dev-admin")
                .build()));
        when(notificationPreferenceResolverService.resolve("dev-admin", "criteria-1"))
                .thenReturn(resolved(List.of(NotificationChannel.EMAIL)));

        useCase.enqueue(alert);

        verify(alertDeliveryRepository, never()).save(any(AlertDeliveryRecord.class));
        verify(taskPublisher, never()).publishTask(any(String.class));
    }

    private ResolvedNotificationPreference resolved(List<NotificationChannel> channels) {
        return ResolvedNotificationPreference.builder()
                .userId("dev-admin")
                .criteriaId("criteria-1")
                .orderedChannels(channels)
                .preferredChannel(channels.get(0))
                .fallbackStrategy(DeliveryFallbackStrategy.FIRST_SUCCESS)
                .criteriaOverrideApplied(false)
                .build();
    }
}
