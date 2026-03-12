package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaAlertRepository extends JpaRepository<AlertEntity, String> {
    List<AlertEntity> findByUserId(String userId);
    Page<AlertEntity> findByUserIdOrderByAlertTimeDesc(String userId, Pageable pageable);
    List<AlertEntity> findByCriteriaIdOrderByAlertTimeDesc(String criteriaId);
    Page<AlertEntity> findByCriteriaIdOrderByAlertTimeDesc(String criteriaId, Pageable pageable);
    Optional<AlertEntity> findByCriteriaIdAndEventKey(String criteriaId, String eventKey);
    List<AlertEntity> findByStatus(String status);

    @Transactional
    @Modifying
    @Query("delete from AlertEntity a where a.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);

    @Transactional
    @Modifying
    @Query("delete from AlertEntity a where a.alertTime is not null and a.alertTime < :cutoff")
    int deleteByAlertTimeBefore(@Param("cutoff") Instant cutoff);
}
