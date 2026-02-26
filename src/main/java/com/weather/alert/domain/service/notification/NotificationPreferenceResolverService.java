package com.weather.alert.domain.service.notification;

import com.weather.alert.domain.model.CriteriaNotificationPreference;
import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.ResolvedNotificationPreference;
import com.weather.alert.domain.model.UserNotificationPreference;
import com.weather.alert.domain.port.CriteriaNotificationPreferenceRepositoryPort;
import com.weather.alert.domain.port.UserNotificationPreferenceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceResolverService {

    private final UserNotificationPreferenceRepositoryPort userNotificationPreferenceRepository;
    private final CriteriaNotificationPreferenceRepositoryPort criteriaNotificationPreferenceRepository;

    public ResolvedNotificationPreference resolve(String userId, String criteriaId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidNotificationPreferenceConfigurationException("userId is required to resolve notification preferences");
        }

        UserNotificationPreference effectiveUserPreference = userNotificationPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> defaultUserPreference(userId));
        NormalizedPreference normalizedUserPreference =
                normalize("user " + userId, effectiveUserPreference.getEnabledChannels(), effectiveUserPreference.getPreferredChannel(),
                        effectiveUserPreference.getFallbackStrategy(), true);

        CriteriaNotificationPreference criteriaPreference = null;
        if (criteriaId != null && !criteriaId.isBlank()) {
            criteriaPreference = criteriaNotificationPreferenceRepository.findByCriteriaId(criteriaId).orElse(null);
        }

        if (criteriaPreference == null || Boolean.TRUE.equals(criteriaPreference.getUseUserDefaults())) {
            if (criteriaPreference != null) {
                validateNoExplicitCriteriaOverride(criteriaPreference, criteriaId);
            }
            return ResolvedNotificationPreference.builder()
                    .userId(userId)
                    .criteriaId(criteriaId)
                    .criteriaOverrideApplied(false)
                    .orderedChannels(normalizedUserPreference.orderedChannels())
                    .preferredChannel(normalizedUserPreference.preferredChannel())
                    .fallbackStrategy(normalizedUserPreference.fallbackStrategy())
                    .build();
        }

        NormalizedPreference normalizedCriteriaPreference =
                normalize("criteria " + criteriaId, criteriaPreference.getEnabledChannels(), criteriaPreference.getPreferredChannel(),
                        criteriaPreference.getFallbackStrategy(), false);
        return ResolvedNotificationPreference.builder()
                .userId(userId)
                .criteriaId(criteriaId)
                .criteriaOverrideApplied(true)
                .orderedChannels(normalizedCriteriaPreference.orderedChannels())
                .preferredChannel(normalizedCriteriaPreference.preferredChannel())
                .fallbackStrategy(normalizedCriteriaPreference.fallbackStrategy())
                .build();
    }

    private void validateNoExplicitCriteriaOverride(CriteriaNotificationPreference criteriaPreference, String criteriaId) {
        boolean hasChannels = criteriaPreference.getEnabledChannels() != null && !criteriaPreference.getEnabledChannels().isEmpty();
        boolean hasPreferred = criteriaPreference.getPreferredChannel() != null;
        boolean hasStrategy = criteriaPreference.getFallbackStrategy() != null;
        if (hasChannels || hasPreferred || hasStrategy) {
            throw new InvalidNotificationPreferenceConfigurationException(
                    "criteria " + criteriaId + " has useUserDefaults=true but also contains explicit channel overrides");
        }
    }

    private UserNotificationPreference defaultUserPreference(String userId) {
        Instant now = Instant.now();
        return UserNotificationPreference.builder()
                .userId(userId)
                .enabledChannels(List.of(NotificationChannel.EMAIL))
                .preferredChannel(NotificationChannel.EMAIL)
                .fallbackStrategy(DeliveryFallbackStrategy.FIRST_SUCCESS)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private NormalizedPreference normalize(
            String scope,
            List<NotificationChannel> rawChannels,
            NotificationChannel preferredChannel,
            DeliveryFallbackStrategy fallbackStrategy,
            boolean allowDefaultChannels) {
        List<NotificationChannel> channels = deduplicate(rawChannels);
        if (channels.isEmpty()) {
            if (allowDefaultChannels) {
                channels = List.of(NotificationChannel.EMAIL);
            } else {
                throw new InvalidNotificationPreferenceConfigurationException(scope + " must define at least one enabled channel");
            }
        }

        NotificationChannel preferred = preferredChannel == null ? channels.get(0) : preferredChannel;
        if (!channels.contains(preferred)) {
            throw new InvalidNotificationPreferenceConfigurationException(
                    scope + " preferred channel " + preferred + " is not present in enabled channels " + channels);
        }

        DeliveryFallbackStrategy strategy =
                fallbackStrategy == null ? DeliveryFallbackStrategy.FIRST_SUCCESS : fallbackStrategy;
        List<NotificationChannel> orderedChannels = prioritize(channels, preferred);
        return new NormalizedPreference(orderedChannels, preferred, strategy);
    }

    private List<NotificationChannel> deduplicate(List<NotificationChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<NotificationChannel> seen = new LinkedHashSet<>();
        for (NotificationChannel channel : channels) {
            if (channel != null) {
                seen.add(channel);
            }
        }
        return new ArrayList<>(seen);
    }

    private List<NotificationChannel> prioritize(List<NotificationChannel> channels, NotificationChannel preferredChannel) {
        List<NotificationChannel> ordered = new ArrayList<>();
        ordered.add(preferredChannel);
        for (NotificationChannel channel : channels) {
            if (!channel.equals(preferredChannel)) {
                ordered.add(channel);
            }
        }
        return ordered;
    }

    private record NormalizedPreference(
            List<NotificationChannel> orderedChannels,
            NotificationChannel preferredChannel,
            DeliveryFallbackStrategy fallbackStrategy) {
    }
}
