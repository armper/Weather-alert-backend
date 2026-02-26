package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.UpdateCriteriaNotificationPreferenceRequest;
import com.weather.alert.application.dto.UpdateUserNotificationPreferenceRequest;
import com.weather.alert.application.exception.CriteriaNotFoundException;
import com.weather.alert.domain.model.CriteriaNotificationPreference;
import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.UserNotificationPreference;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.CriteriaNotificationPreferenceRepositoryPort;
import com.weather.alert.domain.port.UserNotificationPreferenceRepositoryPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import com.weather.alert.domain.service.notification.InvalidNotificationPreferenceConfigurationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManageNotificationPreferencesUseCase {

    private final UserNotificationPreferenceRepositoryPort userNotificationPreferenceRepository;
    private final CriteriaNotificationPreferenceRepositoryPort criteriaNotificationPreferenceRepository;
    private final AlertCriteriaRepositoryPort alertCriteriaRepository;
    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    public UserNotificationPreference getUserPreference(String userId) {
        requireUserId(userId);
        return userNotificationPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> defaultUserPreference(userId));
    }

    @Transactional
    public UserNotificationPreference upsertUserPreference(
            String userId,
            UpdateUserNotificationPreferenceRequest request) {
        requireUserId(userId);
        ensureUserExists(userId);

        List<NotificationChannel> channels = normalizeChannels(request.getEnabledChannels());
        if (channels.isEmpty()) {
            throw new InvalidNotificationPreferenceConfigurationException(
                    "enabledChannels must contain at least one channel");
        }
        NotificationChannel preferredChannel = request.getPreferredChannel();
        if (!channels.contains(preferredChannel)) {
            throw new InvalidNotificationPreferenceConfigurationException(
                    "preferredChannel must be present in enabledChannels");
        }

        DeliveryFallbackStrategy fallbackStrategy = request.getFallbackStrategy() == null
                ? DeliveryFallbackStrategy.FIRST_SUCCESS
                : request.getFallbackStrategy();
        Instant now = Instant.now();
        UserNotificationPreference existing =
                userNotificationPreferenceRepository.findByUserId(userId).orElse(null);

        UserNotificationPreference saved = userNotificationPreferenceRepository.save(UserNotificationPreference.builder()
                .userId(userId)
                .enabledChannels(channels)
                .preferredChannel(preferredChannel)
                .fallbackStrategy(fallbackStrategy)
                .createdAt(existing != null && existing.getCreatedAt() != null ? existing.getCreatedAt() : now)
                .updatedAt(now)
                .build());
        return normalizeUserPreference(saved);
    }

    @Transactional(readOnly = true)
    public CriteriaNotificationPreference getCriteriaPreference(String criteriaId) {
        requireCriteria(criteriaId);
        return criteriaNotificationPreferenceRepository.findByCriteriaId(criteriaId)
                .orElseGet(() -> defaultCriteriaPreference(criteriaId));
    }

    @Transactional
    public CriteriaNotificationPreference upsertCriteriaPreference(
            String criteriaId,
            UpdateCriteriaNotificationPreferenceRequest request) {
        requireCriteria(criteriaId);
        boolean useUserDefaults = request.getUseUserDefaults() == null || request.getUseUserDefaults();
        CriteriaNotificationPreference existing =
                criteriaNotificationPreferenceRepository.findByCriteriaId(criteriaId).orElse(null);

        if (useUserDefaults) {
            criteriaNotificationPreferenceRepository.deleteByCriteriaId(criteriaId);
            return defaultCriteriaPreference(criteriaId);
        }

        List<NotificationChannel> channels = normalizeChannels(request.getEnabledChannels());
        if (channels.isEmpty()) {
            throw new InvalidNotificationPreferenceConfigurationException(
                    "enabledChannels must contain at least one channel when useUserDefaults is false");
        }
        NotificationChannel preferredChannel = request.getPreferredChannel();
        if (preferredChannel == null || !channels.contains(preferredChannel)) {
            throw new InvalidNotificationPreferenceConfigurationException(
                    "preferredChannel must be present in enabledChannels when useUserDefaults is false");
        }
        DeliveryFallbackStrategy fallbackStrategy = request.getFallbackStrategy() == null
                ? DeliveryFallbackStrategy.FIRST_SUCCESS
                : request.getFallbackStrategy();

        Instant now = Instant.now();
        return criteriaNotificationPreferenceRepository.save(CriteriaNotificationPreference.builder()
                .criteriaId(criteriaId)
                .useUserDefaults(false)
                .enabledChannels(channels)
                .preferredChannel(preferredChannel)
                .fallbackStrategy(fallbackStrategy)
                .createdAt(existing != null && existing.getCreatedAt() != null ? existing.getCreatedAt() : now)
                .updatedAt(now)
                .build());
    }

    private void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidNotificationPreferenceConfigurationException(
                    "userId is required for notification preference operations");
        }
    }

    private void requireCriteria(String criteriaId) {
        if (criteriaId == null || criteriaId.isBlank()) {
            throw new InvalidNotificationPreferenceConfigurationException(
                    "criteriaId is required for criteria notification preference operations");
        }
        alertCriteriaRepository.findById(criteriaId)
                .orElseThrow(() -> new CriteriaNotFoundException(criteriaId));
    }

    private void ensureUserExists(String userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new InvalidNotificationPreferenceConfigurationException(
                    "User profile does not exist for " + userId + ". Verify a channel first.");
        }
    }

    private UserNotificationPreference defaultUserPreference(String userId) {
        return UserNotificationPreference.builder()
                .userId(userId)
                .enabledChannels(List.of(NotificationChannel.EMAIL))
                .preferredChannel(NotificationChannel.EMAIL)
                .fallbackStrategy(DeliveryFallbackStrategy.FIRST_SUCCESS)
                .build();
    }

    private CriteriaNotificationPreference defaultCriteriaPreference(String criteriaId) {
        return CriteriaNotificationPreference.builder()
                .criteriaId(criteriaId)
                .useUserDefaults(true)
                .enabledChannels(List.of())
                .preferredChannel(null)
                .fallbackStrategy(null)
                .build();
    }

    private UserNotificationPreference normalizeUserPreference(UserNotificationPreference preference) {
        List<NotificationChannel> channels = normalizeChannels(preference.getEnabledChannels());
        if (channels.isEmpty()) {
            channels = List.of(NotificationChannel.EMAIL);
        }
        NotificationChannel preferred = preference.getPreferredChannel();
        if (preferred == null || !channels.contains(preferred)) {
            preferred = channels.get(0);
        }
        DeliveryFallbackStrategy fallback = preference.getFallbackStrategy() == null
                ? DeliveryFallbackStrategy.FIRST_SUCCESS
                : preference.getFallbackStrategy();

        return UserNotificationPreference.builder()
                .userId(preference.getUserId())
                .enabledChannels(channels)
                .preferredChannel(preferred)
                .fallbackStrategy(fallback)
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    private List<NotificationChannel> normalizeChannels(List<NotificationChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<NotificationChannel> deduped = new LinkedHashSet<>();
        for (NotificationChannel channel : channels) {
            if (channel != null) {
                deduped.add(channel);
            }
        }
        return new ArrayList<>(deduped);
    }
}
