package com.weather.alert.domain.service.notification;

import com.weather.alert.domain.model.CriteriaNotificationPreference;
import com.weather.alert.domain.model.ChannelVerification;
import com.weather.alert.domain.model.ChannelVerificationStatus;
import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.ResolvedNotificationPreference;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.UserNotificationPreference;
import com.weather.alert.domain.port.ChannelVerificationRepositoryPort;
import com.weather.alert.domain.port.CriteriaNotificationPreferenceRepositoryPort;
import com.weather.alert.domain.port.UserNotificationPreferenceRepositoryPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceResolverServiceTest {

    @Mock
    private UserNotificationPreferenceRepositoryPort userPreferenceRepository;

    @Mock
    private CriteriaNotificationPreferenceRepositoryPort criteriaPreferenceRepository;

    @Mock
    private ChannelVerificationRepositoryPort channelVerificationRepository;

    @Mock
    private UserRepositoryPort userRepository;

    private NotificationPreferenceResolverService service;

    @BeforeEach
    void setUp() {
        service = new NotificationPreferenceResolverService(
                userPreferenceRepository,
                criteriaPreferenceRepository,
                channelVerificationRepository,
                userRepository);
    }

    @Test
    void shouldReturnDefaultPreferenceWhenNoneConfigured() {
        when(userPreferenceRepository.findByUserId("dev-user")).thenReturn(Optional.empty());
        when(criteriaPreferenceRepository.findByCriteriaId("criteria-1")).thenReturn(Optional.empty());
        when(userRepository.findById("dev-user")).thenReturn(Optional.of(User.builder()
                .id("dev-user")
                .email("dev-user@example.com")
                .build()));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-user",
                NotificationChannel.EMAIL,
                "dev-user@example.com")).thenReturn(Optional.of(verified(NotificationChannel.EMAIL, "dev-user@example.com")));

        ResolvedNotificationPreference result = service.resolve("dev-user", "criteria-1");

        assertFalse(result.isCriteriaOverrideApplied());
        assertEquals(NotificationChannel.EMAIL, result.getPreferredChannel());
        assertEquals(List.of(NotificationChannel.EMAIL), result.getOrderedChannels());
        assertEquals(DeliveryFallbackStrategy.FIRST_SUCCESS, result.getFallbackStrategy());
    }

    @Test
    void shouldUseUserPreferenceWhenNoCriteriaOverrideConfigured() {
        when(userPreferenceRepository.findByUserId("dev-user")).thenReturn(Optional.of(
                UserNotificationPreference.builder()
                        .userId("dev-user")
                        .enabledChannels(List.of(NotificationChannel.SMS, NotificationChannel.EMAIL))
                        .preferredChannel(NotificationChannel.EMAIL)
                        .fallbackStrategy(DeliveryFallbackStrategy.FIRST_SUCCESS)
                        .build()));
        when(criteriaPreferenceRepository.findByCriteriaId("criteria-1")).thenReturn(Optional.empty());
        when(userRepository.findById("dev-user")).thenReturn(Optional.of(User.builder()
                .id("dev-user")
                .email("dev-user@example.com")
                .phoneNumber("+14075551234")
                .build()));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-user",
                NotificationChannel.EMAIL,
                "dev-user@example.com")).thenReturn(Optional.of(verified(NotificationChannel.EMAIL, "dev-user@example.com")));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-user",
                NotificationChannel.SMS,
                "+14075551234")).thenReturn(Optional.of(verified(NotificationChannel.SMS, "+14075551234")));

        ResolvedNotificationPreference result = service.resolve("dev-user", "criteria-1");

        assertFalse(result.isCriteriaOverrideApplied());
        assertEquals(List.of(NotificationChannel.EMAIL, NotificationChannel.SMS), result.getOrderedChannels());
        assertEquals(NotificationChannel.EMAIL, result.getPreferredChannel());
    }

    @Test
    void shouldApplyCriteriaOverrideWhenUseUserDefaultsIsFalse() {
        when(userPreferenceRepository.findByUserId("dev-user")).thenReturn(Optional.of(
                UserNotificationPreference.builder()
                        .userId("dev-user")
                        .enabledChannels(List.of(NotificationChannel.EMAIL))
                        .preferredChannel(NotificationChannel.EMAIL)
                        .fallbackStrategy(DeliveryFallbackStrategy.FIRST_SUCCESS)
                        .build()));
        when(criteriaPreferenceRepository.findByCriteriaId("criteria-1")).thenReturn(Optional.of(
                CriteriaNotificationPreference.builder()
                        .criteriaId("criteria-1")
                        .useUserDefaults(false)
                        .enabledChannels(List.of(NotificationChannel.SMS, NotificationChannel.EMAIL))
                        .preferredChannel(NotificationChannel.SMS)
                        .fallbackStrategy(DeliveryFallbackStrategy.ALL_ENABLED)
                        .build()));
        when(userRepository.findById("dev-user")).thenReturn(Optional.of(User.builder()
                .id("dev-user")
                .email("dev-user@example.com")
                .phoneNumber("+14075551234")
                .build()));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-user",
                NotificationChannel.EMAIL,
                "dev-user@example.com")).thenReturn(Optional.of(verified(NotificationChannel.EMAIL, "dev-user@example.com")));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-user",
                NotificationChannel.SMS,
                "+14075551234")).thenReturn(Optional.of(verified(NotificationChannel.SMS, "+14075551234")));

        ResolvedNotificationPreference result = service.resolve("dev-user", "criteria-1");

        assertTrue(result.isCriteriaOverrideApplied());
        assertEquals(List.of(NotificationChannel.SMS, NotificationChannel.EMAIL), result.getOrderedChannels());
        assertEquals(NotificationChannel.SMS, result.getPreferredChannel());
        assertEquals(DeliveryFallbackStrategy.ALL_ENABLED, result.getFallbackStrategy());
    }

    @Test
    void shouldUseFirstEnabledChannelWhenPreferredChannelMissing() {
        when(userPreferenceRepository.findByUserId("dev-user")).thenReturn(Optional.of(
                UserNotificationPreference.builder()
                        .userId("dev-user")
                        .enabledChannels(List.of(NotificationChannel.SMS, NotificationChannel.EMAIL))
                        .preferredChannel(null)
                        .fallbackStrategy(null)
                        .build()));
        when(criteriaPreferenceRepository.findByCriteriaId("criteria-1")).thenReturn(Optional.empty());
        when(userRepository.findById("dev-user")).thenReturn(Optional.of(User.builder()
                .id("dev-user")
                .email("dev-user@example.com")
                .phoneNumber("+14075551234")
                .build()));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-user",
                NotificationChannel.EMAIL,
                "dev-user@example.com")).thenReturn(Optional.of(verified(NotificationChannel.EMAIL, "dev-user@example.com")));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-user",
                NotificationChannel.SMS,
                "+14075551234")).thenReturn(Optional.of(verified(NotificationChannel.SMS, "+14075551234")));

        ResolvedNotificationPreference result = service.resolve("dev-user", "criteria-1");

        assertEquals(NotificationChannel.SMS, result.getPreferredChannel());
        assertEquals(List.of(NotificationChannel.SMS, NotificationChannel.EMAIL), result.getOrderedChannels());
        assertEquals(DeliveryFallbackStrategy.FIRST_SUCCESS, result.getFallbackStrategy());
    }

    @Test
    void shouldThrowWhenCriteriaOverrideHasNoEnabledChannels() {
        when(userPreferenceRepository.findByUserId("dev-user")).thenReturn(Optional.empty());
        when(criteriaPreferenceRepository.findByCriteriaId("criteria-1")).thenReturn(Optional.of(
                CriteriaNotificationPreference.builder()
                        .criteriaId("criteria-1")
                        .useUserDefaults(false)
                        .enabledChannels(List.of())
                        .build()));

        assertThrows(
                InvalidNotificationPreferenceConfigurationException.class,
                () -> service.resolve("dev-user", "criteria-1"));
    }

    @Test
    void shouldThrowWhenPreferredChannelNotInEnabledChannels() {
        when(userPreferenceRepository.findByUserId("dev-user")).thenReturn(Optional.of(
                UserNotificationPreference.builder()
                        .userId("dev-user")
                        .enabledChannels(List.of(NotificationChannel.EMAIL))
                        .preferredChannel(NotificationChannel.SMS)
                        .build()));

        assertThrows(
                InvalidNotificationPreferenceConfigurationException.class,
                () -> service.resolve("dev-user", "criteria-1"));
    }

    @Test
    void shouldThrowWhenUseUserDefaultsIsTrueButCriteriaContainsOverrides() {
        when(userPreferenceRepository.findByUserId("dev-user")).thenReturn(Optional.empty());
        when(criteriaPreferenceRepository.findByCriteriaId("criteria-1")).thenReturn(Optional.of(
                CriteriaNotificationPreference.builder()
                        .criteriaId("criteria-1")
                        .useUserDefaults(true)
                        .enabledChannels(List.of(NotificationChannel.SMS))
                        .build()));

        assertThrows(
                InvalidNotificationPreferenceConfigurationException.class,
                () -> service.resolve("dev-user", "criteria-1"));
    }

    @Test
    void shouldThrowWhenNoVerifiedChannelIsAvailable() {
        when(userPreferenceRepository.findByUserId("dev-user")).thenReturn(Optional.of(
                UserNotificationPreference.builder()
                        .userId("dev-user")
                        .enabledChannels(List.of(NotificationChannel.EMAIL))
                        .preferredChannel(NotificationChannel.EMAIL)
                        .build()));
        when(criteriaPreferenceRepository.findByCriteriaId("criteria-1")).thenReturn(Optional.empty());
        when(userRepository.findById("dev-user")).thenReturn(Optional.of(User.builder()
                .id("dev-user")
                .email("dev-user@example.com")
                .build()));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-user",
                NotificationChannel.EMAIL,
                "dev-user@example.com")).thenReturn(Optional.empty());

        assertThrows(
                InvalidNotificationPreferenceConfigurationException.class,
                () -> service.resolve("dev-user", "criteria-1"));
    }

    @Test
    void shouldRemoveUnverifiedPreferredChannelAndFallbackToVerifiedOne() {
        when(userPreferenceRepository.findByUserId("dev-user")).thenReturn(Optional.of(
                UserNotificationPreference.builder()
                        .userId("dev-user")
                        .enabledChannels(List.of(NotificationChannel.SMS, NotificationChannel.EMAIL))
                        .preferredChannel(NotificationChannel.SMS)
                        .fallbackStrategy(DeliveryFallbackStrategy.FIRST_SUCCESS)
                        .build()));
        when(criteriaPreferenceRepository.findByCriteriaId("criteria-1")).thenReturn(Optional.empty());
        when(userRepository.findById("dev-user")).thenReturn(Optional.of(User.builder()
                .id("dev-user")
                .email("dev-user@example.com")
                .phoneNumber("+14075551234")
                .build()));
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-user",
                NotificationChannel.SMS,
                "+14075551234")).thenReturn(Optional.empty());
        when(channelVerificationRepository.findByUserIdAndChannelAndDestination(
                "dev-user",
                NotificationChannel.EMAIL,
                "dev-user@example.com")).thenReturn(Optional.of(verified(NotificationChannel.EMAIL, "dev-user@example.com")));

        ResolvedNotificationPreference result = service.resolve("dev-user", "criteria-1");

        assertEquals(List.of(NotificationChannel.EMAIL), result.getOrderedChannels());
        assertEquals(NotificationChannel.EMAIL, result.getPreferredChannel());
    }

    private ChannelVerification verified(NotificationChannel channel, String destination) {
        return ChannelVerification.builder()
                .id("verification-" + channel.name())
                .userId("dev-user")
                .channel(channel)
                .destination(destination)
                .status(ChannelVerificationStatus.VERIFIED)
                .build();
    }
}
