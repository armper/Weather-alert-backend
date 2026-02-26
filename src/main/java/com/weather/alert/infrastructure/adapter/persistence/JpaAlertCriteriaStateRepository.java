package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public interface JpaAlertCriteriaStateRepository extends JpaRepository<AlertCriteriaStateEntity, String> {

    @Transactional
    @Modifying
    @Query("delete from AlertCriteriaStateEntity s where s.updatedAt < :cutoff")
    int deleteByUpdatedAtBefore(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query(
            value = "delete from criteria_state cs where not exists (" +
                    "select 1 from alert_criteria ac where ac.id = cs.criteria_id" +
                    ")",
            nativeQuery = true)
    int deleteOrphanedStates();
}
