package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreferenceEntity, String> {
}
