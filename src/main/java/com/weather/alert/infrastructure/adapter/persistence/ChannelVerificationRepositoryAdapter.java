package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.ChannelVerification;
import com.weather.alert.domain.model.ChannelVerificationStatus;
import com.weather.alert.domain.model.NotificationChannel;
import com.weather.alert.domain.port.ChannelVerificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChannelVerificationRepositoryAdapter implements ChannelVerificationRepositoryPort {

    private final JpaChannelVerificationRepository jpaRepository;

    @Override
    public ChannelVerification save(ChannelVerification verification) {
        ChannelVerificationEntity entity = toEntity(verification);
        ChannelVerificationEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ChannelVerification> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ChannelVerification> findByUserIdAndChannelAndDestination(
            String userId,
            NotificationChannel channel,
            String destination) {
        return jpaRepository.findTopByUserIdAndChannelAndDestinationOrderByUpdatedAtDesc(
                userId,
                channel.name(),
                destination).map(this::toDomain);
    }

    private ChannelVerificationEntity toEntity(ChannelVerification verification) {
        return ChannelVerificationEntity.builder()
                .id(verification.getId())
                .userId(verification.getUserId())
                .channel(verification.getChannel().name())
                .destination(verification.getDestination())
                .status(verification.getStatus().name())
                .verificationTokenHash(verification.getVerificationTokenHash())
                .tokenExpiresAt(verification.getTokenExpiresAt())
                .verifiedAt(verification.getVerifiedAt())
                .createdAt(verification.getCreatedAt())
                .updatedAt(verification.getUpdatedAt())
                .build();
    }

    private ChannelVerification toDomain(ChannelVerificationEntity entity) {
        return ChannelVerification.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .channel(NotificationChannel.valueOf(entity.getChannel()))
                .destination(entity.getDestination())
                .status(ChannelVerificationStatus.valueOf(entity.getStatus()))
                .verificationTokenHash(entity.getVerificationTokenHash())
                .tokenExpiresAt(entity.getTokenExpiresAt())
                .verifiedAt(entity.getVerifiedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
