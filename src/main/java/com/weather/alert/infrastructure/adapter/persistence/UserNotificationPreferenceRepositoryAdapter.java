package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.model.UserNotificationPreference;
import com.weather.alert.domain.port.UserNotificationPreferenceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserNotificationPreferenceRepositoryAdapter implements UserNotificationPreferenceRepositoryPort {

    private final JpaUserNotificationPreferenceRepository jpaRepository;

    @Override
    public UserNotificationPreference save(UserNotificationPreference preference) {
        UserNotificationPreferenceEntity entity = toEntity(preference);
        UserNotificationPreferenceEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<UserNotificationPreference> findByUserId(String userId) {
        return jpaRepository.findById(userId).map(this::toDomain);
    }

    private UserNotificationPreferenceEntity toEntity(UserNotificationPreference preference) {
        List<NotificationChannel> enabledChannels = normalizeChannels(preference.getEnabledChannels());
        NotificationChannel preferredChannel =
                preference.getPreferredChannel() != null ? preference.getPreferredChannel() : enabledChannels.get(0);
        DeliveryFallbackStrategy fallbackStrategy =
                preference.getFallbackStrategy() != null ? preference.getFallbackStrategy() : DeliveryFallbackStrategy.FIRST_SUCCESS;

        return UserNotificationPreferenceEntity.builder()
                .userId(preference.getUserId())
                .enabledChannels(NotificationChannelCodec.encode(enabledChannels))
                .preferredChannel(preferredChannel.name())
                .fallbackStrategy(fallbackStrategy.name())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    private UserNotificationPreference toDomain(UserNotificationPreferenceEntity entity) {
        List<NotificationChannel> channels = normalizeChannels(NotificationChannelCodec.decode(entity.getEnabledChannels()));
        NotificationChannel preferred = entity.getPreferredChannel() == null || entity.getPreferredChannel().isBlank()
                ? channels.get(0)
                : NotificationChannel.valueOf(entity.getPreferredChannel());
        DeliveryFallbackStrategy fallbackStrategy = entity.getFallbackStrategy() == null || entity.getFallbackStrategy().isBlank()
                ? DeliveryFallbackStrategy.FIRST_SUCCESS
                : DeliveryFallbackStrategy.valueOf(entity.getFallbackStrategy());

        return UserNotificationPreference.builder()
                .userId(entity.getUserId())
                .enabledChannels(channels)
                .preferredChannel(preferred)
                .fallbackStrategy(fallbackStrategy)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private List<NotificationChannel> normalizeChannels(List<NotificationChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            return List.of(NotificationChannel.EMAIL);
        }
        return channels;
    }
}
