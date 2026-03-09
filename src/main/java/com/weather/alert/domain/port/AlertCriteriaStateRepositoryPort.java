package com.weather.alert.domain.port;

import com.weather.alert.domain.model.AlertCriteriaState;

import java.time.Instant;
import java.util.Optional;

/**
 * Port for persisted alert criteria anti-spam state.
 */
public interface AlertCriteriaStateRepositoryPort {

    Optional<AlertCriteriaState> findByCriteriaId(String criteriaId);

    AlertCriteriaState save(AlertCriteriaState state);

    int deleteByUpdatedAtBefore(Instant cutoff);

    int deleteOrphanedStates();
}
