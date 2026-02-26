package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.UpdateCriteriaNotificationPreferenceRequest;
import com.weather.alert.application.dto.UpdateUserNotificationPreferenceRequest;
import com.weather.alert.application.exception.CriteriaNotFoundException;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.CriteriaNotificationPreference;
import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.UserNotificationPreference;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.CriteriaNotificationPreferenceRepositoryPort;
import com.weather.alert.domain.port.UserNotificationPreferenceRepositoryPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import com.weather.alert.domain.service.notification.InvalidNotificationPreferenceConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageNotificationPreferencesUseCaseTest {

    @Mock
    private UserNotificationPreferenceRepositoryPort userNotificationPreferenceRepository;

    @Mock
    private CriteriaNotificationPreferenceRepositoryPort criteriaNotificationPreferenceRepository;

    @Mock
    private AlertCriteriaRepositoryPort alertCriteriaRepository;

    @Mock
    private UserRepositoryPort userRepository;

    private ManageNotificationPreferencesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ManageNotificationPreferencesUseCase(
                userNotificationPreferenceRepository,
                criteriaNotificationPreferenceRepository,
                alertCriteriaRepository,
                userRepository);
    }

    @Test
    void shouldReturnDefaultUserPreferenceWhenNoRecordExists() {
        when(userNotificationPreferenceRepository.findByUserId("dev-user")).thenReturn(Optional.empty());

        UserNotificationPreference preference = useCase.getUserPreference("dev-user");

        assertEquals("dev-user", preference.getUserId());
        assertEquals(List.of(NotificationChannel.EMAIL), preference.getEnabledChannels());
        assertEquals(NotificationChannel.EMAIL, preference.getPreferredChannel());
        assertEquals(DeliveryFallbackStrategy.FIRST_SUCCESS, preference.getFallbackStrategy());
    }

    @Test
    void shouldPersistUserNotificationPreference() {
        UpdateUserNotificationPreferenceRequest request = new UpdateUserNotificationPreferenceRequest();
        request.setEnabledChannels(List.of(NotificationChannel.SMS, NotificationChannel.EMAIL));
        request.setPreferredChannel(NotificationChannel.SMS);
        request.setFallbackStrategy(DeliveryFallbackStrategy.ALL_ENABLED);

        when(userRepository.findById("dev-user")).thenReturn(Optional.of(User.builder().id("dev-user").build()));
        when(userNotificationPreferenceRepository.findByUserId("dev-user")).thenReturn(Optional.empty());
        when(userNotificationPreferenceRepository.save(any(UserNotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserNotificationPreference saved = useCase.upsertUserPreference("dev-user", request);

        assertEquals(List.of(NotificationChannel.SMS, NotificationChannel.EMAIL), saved.getEnabledChannels());
        assertEquals(NotificationChannel.SMS, saved.getPreferredChannel());
        assertEquals(DeliveryFallbackStrategy.ALL_ENABLED, saved.getFallbackStrategy());
        verify(userNotificationPreferenceRepository).save(any(UserNotificationPreference.class));
    }

    @Test
    void shouldRejectUserPreferenceWhenUserProfileMissing() {
        UpdateUserNotificationPreferenceRequest request = new UpdateUserNotificationPreferenceRequest();
        request.setEnabledChannels(List.of(NotificationChannel.EMAIL));
        request.setPreferredChannel(NotificationChannel.EMAIL);

        when(userRepository.findById("dev-user")).thenReturn(Optional.empty());

        assertThrows(
                InvalidNotificationPreferenceConfigurationException.class,
                () -> useCase.upsertUserPreference("dev-user", request));
        verify(userNotificationPreferenceRepository, never()).save(any(UserNotificationPreference.class));
    }

    @Test
    void shouldReturnDefaultCriteriaPreferenceWhenNoOverrideExists() {
        when(alertCriteriaRepository.findById("criteria-1")).thenReturn(Optional.of(AlertCriteria.builder()
                .id("criteria-1")
                .userId("dev-user")
                .build()));
        when(criteriaNotificationPreferenceRepository.findByCriteriaId("criteria-1")).thenReturn(Optional.empty());

        CriteriaNotificationPreference preference = useCase.getCriteriaPreference("criteria-1");

        assertTrue(preference.getUseUserDefaults());
        assertEquals("criteria-1", preference.getCriteriaId());
        assertTrue(preference.getEnabledChannels().isEmpty());
    }

    @Test
    void shouldDeleteCriteriaOverrideWhenSwitchingToUserDefaults() {
        UpdateCriteriaNotificationPreferenceRequest request = new UpdateCriteriaNotificationPreferenceRequest();
        request.setUseUserDefaults(true);

        when(alertCriteriaRepository.findById("criteria-1")).thenReturn(Optional.of(AlertCriteria.builder()
                .id("criteria-1")
                .userId("dev-user")
                .build()));
        when(criteriaNotificationPreferenceRepository.findByCriteriaId("criteria-1"))
                .thenReturn(Optional.of(CriteriaNotificationPreference.builder()
                        .criteriaId("criteria-1")
                        .useUserDefaults(false)
                        .enabledChannels(List.of(NotificationChannel.EMAIL))
                        .preferredChannel(NotificationChannel.EMAIL)
                        .build()));

        CriteriaNotificationPreference saved = useCase.upsertCriteriaPreference("criteria-1", request);

        verify(criteriaNotificationPreferenceRepository).deleteByCriteriaId("criteria-1");
        verify(criteriaNotificationPreferenceRepository, never()).save(any(CriteriaNotificationPreference.class));
        assertTrue(saved.getUseUserDefaults());
        assertTrue(saved.getEnabledChannels().isEmpty());
    }

    @Test
    void shouldPersistCriteriaOverrideWhenExplicitConfigProvided() {
        UpdateCriteriaNotificationPreferenceRequest request = new UpdateCriteriaNotificationPreferenceRequest();
        request.setUseUserDefaults(false);
        request.setEnabledChannels(List.of(NotificationChannel.EMAIL, NotificationChannel.SMS));
        request.setPreferredChannel(NotificationChannel.EMAIL);
        request.setFallbackStrategy(DeliveryFallbackStrategy.FIRST_SUCCESS);

        when(alertCriteriaRepository.findById("criteria-1")).thenReturn(Optional.of(AlertCriteria.builder()
                .id("criteria-1")
                .userId("dev-user")
                .build()));
        when(criteriaNotificationPreferenceRepository.findByCriteriaId("criteria-1")).thenReturn(Optional.empty());
        when(criteriaNotificationPreferenceRepository.save(any(CriteriaNotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CriteriaNotificationPreference saved = useCase.upsertCriteriaPreference("criteria-1", request);

        assertEquals(List.of(NotificationChannel.EMAIL, NotificationChannel.SMS), saved.getEnabledChannels());
        assertEquals(NotificationChannel.EMAIL, saved.getPreferredChannel());
        assertEquals(DeliveryFallbackStrategy.FIRST_SUCCESS, saved.getFallbackStrategy());
        assertEquals(Boolean.FALSE, saved.getUseUserDefaults());
    }

    @Test
    void shouldFailWhenCriteriaDoesNotExist() {
        when(alertCriteriaRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(CriteriaNotFoundException.class, () -> useCase.getCriteriaPreference("missing"));
    }
}

