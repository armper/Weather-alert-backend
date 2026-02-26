package com.weather.alert.domain.service.notification;

import com.weather.alert.domain.model.CriteriaNotificationPreference;
import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.ResolvedNotificationPreference;
import com.weather.alert.domain.model.ChannelVerificationStatus;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.model.UserNotificationPreference;
import com.weather.alert.domain.port.ChannelVerificationRepositoryPort;
import com.weather.alert.domain.port.CriteriaNotificationPreferenceRepositoryPort;
import com.weather.alert.domain.port.UserNotificationPreferenceRepositoryPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceResolverService {

    private final UserNotificationPreferenceRepositoryPort userNotificationPreferenceRepository;
    private final CriteriaNotificationPreferenceRepositoryPort criteriaNotificationPreferenceRepository;
    private final ChannelVerificationRepositoryPort channelVerificationRepository;
    private final UserRepositoryPort userRepository;

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
            ResolvedNotificationPreference resolved = ResolvedNotificationPreference.builder()
                    .userId(userId)
                    .criteriaId(criteriaId)
                    .criteriaOverrideApplied(false)
                    .orderedChannels(normalizedUserPreference.orderedChannels())
                    .preferredChannel(normalizedUserPreference.preferredChannel())
                    .fallbackStrategy(normalizedUserPreference.fallbackStrategy())
                    .build();
            return applyVerificationFilter(resolved);
        }

        NormalizedPreference normalizedCriteriaPreference =
                normalize("criteria " + criteriaId, criteriaPreference.getEnabledChannels(), criteriaPreference.getPreferredChannel(),
                        criteriaPreference.getFallbackStrategy(), false);
        ResolvedNotificationPreference resolved = ResolvedNotificationPreference.builder()
                .userId(userId)
                .criteriaId(criteriaId)
                .criteriaOverrideApplied(true)
                .orderedChannels(normalizedCriteriaPreference.orderedChannels())
                .preferredChannel(normalizedCriteriaPreference.preferredChannel())
                .fallbackStrategy(normalizedCriteriaPreference.fallbackStrategy())
                .build();
        return applyVerificationFilter(resolved);
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

    private ResolvedNotificationPreference applyVerificationFilter(ResolvedNotificationPreference resolved) {
        Optional<User> user = userRepository.findById(resolved.getUserId());
        List<NotificationChannel> verifiedChannels = resolved.getOrderedChannels().stream()
                .filter(channel -> isChannelVerified(resolved.getUserId(), channel, user))
                .toList();

        if (verifiedChannels.isEmpty()) {
            throw new InvalidNotificationPreferenceConfigurationException(
                    "no verified notification channels are available for user " + resolved.getUserId());
        }

        NotificationChannel preferred = resolved.getPreferredChannel();
        if (preferred == null || !verifiedChannels.contains(preferred)) {
            preferred = verifiedChannels.get(0);
        }

        return ResolvedNotificationPreference.builder()
                .userId(resolved.getUserId())
                .criteriaId(resolved.getCriteriaId())
                .criteriaOverrideApplied(resolved.isCriteriaOverrideApplied())
                .orderedChannels(prioritize(verifiedChannels, preferred))
                .preferredChannel(preferred)
                .fallbackStrategy(resolved.getFallbackStrategy())
                .build();
    }

    private boolean isChannelVerified(String userId, NotificationChannel channel, Optional<User> user) {
        if (channel == NotificationChannel.PUSH) {
            return true;
        }
        String destination = resolveDestination(channel, user);
        if (destination == null || destination.isBlank()) {
            return false;
        }
        return channelVerificationRepository.findByUserIdAndChannelAndDestination(userId, channel, destination)
                .filter(verification -> verification.getStatus() == ChannelVerificationStatus.VERIFIED)
                .isPresent();
    }

    private String resolveDestination(NotificationChannel channel, Optional<User> user) {
        if (user.isEmpty()) {
            return null;
        }
        if (channel == NotificationChannel.EMAIL) {
            String email = user.get().getEmail();
            return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        }
        if (channel == NotificationChannel.SMS) {
            String phone = user.get().getPhoneNumber();
            return phone == null ? null : phone.trim();
        }
        return null;
    }

    private record NormalizedPreference(
            List<NotificationChannel> orderedChannels,
            NotificationChannel preferredChannel,
            DeliveryFallbackStrategy fallbackStrategy) {
    }
}
