package com.weather.alert.domain.port;

import com.weather.alert.domain.model.UserNotificationPreference;

import java.util.Optional;

public interface UserNotificationPreferenceRepositoryPort {

    UserNotificationPreference save(UserNotificationPreference preference);

    Optional<UserNotificationPreference> findByUserId(String userId);
}
