package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaChannelVerificationRepository extends JpaRepository<ChannelVerificationEntity, String> {

    Optional<ChannelVerificationEntity> findTopByUserIdAndChannelAndDestinationOrderByUpdatedAtDesc(
            String userId,
            String channel,
            String destination);
}
