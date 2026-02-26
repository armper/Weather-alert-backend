package com.weather.alert.domain.port;

import com.weather.alert.domain.model.CriteriaNotificationPreference;

import java.util.Optional;

public interface CriteriaNotificationPreferenceRepositoryPort {

    CriteriaNotificationPreference save(CriteriaNotificationPreference preference);

    Optional<CriteriaNotificationPreference> findByCriteriaId(String criteriaId);

    void deleteByCriteriaId(String criteriaId);
}
