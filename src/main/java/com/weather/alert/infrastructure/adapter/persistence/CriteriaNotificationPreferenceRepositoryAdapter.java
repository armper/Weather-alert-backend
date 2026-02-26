package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.CriteriaNotificationPreference;
import com.weather.alert.domain.model.DeliveryFallbackStrategy;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.port.CriteriaNotificationPreferenceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CriteriaNotificationPreferenceRepositoryAdapter implements CriteriaNotificationPreferenceRepositoryPort {

    private final JpaCriteriaNotificationPreferenceRepository jpaRepository;

    @Override
    public CriteriaNotificationPreference save(CriteriaNotificationPreference preference) {
        CriteriaNotificationPreferenceEntity entity = toEntity(preference);
        CriteriaNotificationPreferenceEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<CriteriaNotificationPreference> findByCriteriaId(String criteriaId) {
        return jpaRepository.findById(criteriaId).map(this::toDomain);
    }

    @Override
    public void deleteByCriteriaId(String criteriaId) {
        jpaRepository.deleteById(criteriaId);
    }

    private CriteriaNotificationPreferenceEntity toEntity(CriteriaNotificationPreference preference) {
        List<NotificationChannel> enabledChannels = preference.getEnabledChannels() == null
                ? List.of()
                : preference.getEnabledChannels();

        return CriteriaNotificationPreferenceEntity.builder()
                .criteriaId(preference.getCriteriaId())
                .useUserDefaults(preference.getUseUserDefaults() == null || preference.getUseUserDefaults())
                .enabledChannels(NotificationChannelCodec.encode(enabledChannels))
                .preferredChannel(preference.getPreferredChannel() != null ? preference.getPreferredChannel().name() : null)
                .fallbackStrategy(preference.getFallbackStrategy() != null ? preference.getFallbackStrategy().name() : null)
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    private CriteriaNotificationPreference toDomain(CriteriaNotificationPreferenceEntity entity) {
        return CriteriaNotificationPreference.builder()
                .criteriaId(entity.getCriteriaId())
                .useUserDefaults(entity.getUseUserDefaults())
                .enabledChannels(NotificationChannelCodec.decode(entity.getEnabledChannels()))
                .preferredChannel(entity.getPreferredChannel() != null
                        ? NotificationChannel.valueOf(entity.getPreferredChannel())
                        : null)
                .fallbackStrategy(entity.getFallbackStrategy() != null
                        ? DeliveryFallbackStrategy.valueOf(entity.getFallbackStrategy())
                        : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
