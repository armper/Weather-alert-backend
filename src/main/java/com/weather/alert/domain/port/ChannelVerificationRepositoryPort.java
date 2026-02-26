package com.weather.alert.domain.port;

import com.weather.alert.domain.model.ChannelVerification;
import com.weather.alert.domain.model.NotificationChannel;

import java.util.Optional;

public interface ChannelVerificationRepositoryPort {

    ChannelVerification save(ChannelVerification verification);

    Optional<ChannelVerification> findById(String id);

    Optional<ChannelVerification> findByUserIdAndChannelAndDestination(
            String userId,
            NotificationChannel channel,
            String destination);
}
