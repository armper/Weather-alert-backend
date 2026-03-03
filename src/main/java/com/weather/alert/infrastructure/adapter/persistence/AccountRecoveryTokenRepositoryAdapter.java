package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.AccountRecoveryPurpose;
import com.weather.alert.domain.model.AccountRecoveryToken;
import com.weather.alert.domain.port.AccountRecoveryTokenRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AccountRecoveryTokenRepositoryAdapter implements AccountRecoveryTokenRepositoryPort {

    private final JpaAccountRecoveryTokenRepository jpaRepository;

    @Override
    public AccountRecoveryToken save(AccountRecoveryToken token) {
        AccountRecoveryTokenEntity saved = jpaRepository.save(toEntity(token));
        return toDomain(saved);
    }

    @Override
    public Optional<AccountRecoveryToken> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<AccountRecoveryToken> findLatestByUserIdAndPurpose(String userId, AccountRecoveryPurpose purpose) {
        return jpaRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose.name())
                .map(this::toDomain);
    }

    private AccountRecoveryTokenEntity toEntity(AccountRecoveryToken token) {
        return AccountRecoveryTokenEntity.builder()
                .id(token.getId())
                .userId(token.getUserId())
                .purpose(token.getPurpose().name())
                .tokenHash(token.getTokenHash())
                .expiresAt(token.getExpiresAt())
                .usedAt(token.getUsedAt())
                .createdAt(token.getCreatedAt())
                .updatedAt(token.getUpdatedAt())
                .build();
    }

    private AccountRecoveryToken toDomain(AccountRecoveryTokenEntity entity) {
        return AccountRecoveryToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .purpose(AccountRecoveryPurpose.valueOf(entity.getPurpose()))
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .usedAt(entity.getUsedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
