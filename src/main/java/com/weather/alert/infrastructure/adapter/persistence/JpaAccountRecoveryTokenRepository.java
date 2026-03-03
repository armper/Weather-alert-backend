package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaAccountRecoveryTokenRepository extends JpaRepository<AccountRecoveryTokenEntity, String> {

    Optional<AccountRecoveryTokenEntity> findTopByUserIdAndPurposeOrderByCreatedAtDesc(String userId, String purpose);
}
