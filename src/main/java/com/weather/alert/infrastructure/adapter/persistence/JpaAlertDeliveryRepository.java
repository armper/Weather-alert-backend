package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaAlertDeliveryRepository extends JpaRepository<AlertDeliveryEntity, String> {

    List<String> CLAIMABLE_STATUSES = List.of("PENDING", "RETRY_SCHEDULED");

    Optional<AlertDeliveryEntity> findByAlertIdAndChannel(String alertId, String channel);

    List<AlertDeliveryEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            List<String> statuses,
            Instant nextAttemptAt,
            Pageable pageable);

    @Transactional
    @Modifying
    @Query("""
            update AlertDeliveryEntity d
            set d.status = 'IN_PROGRESS',
                d.updatedAt = :claimedAt
            where d.id = :id
              and d.status in :claimableStatuses
              and (d.nextAttemptAt is null or d.nextAttemptAt <= :claimedAt)
            """)
    int claimForDelivery(
            @Param("id") String id,
            @Param("claimedAt") Instant claimedAt,
            @Param("claimableStatuses") List<String> claimableStatuses);

    @Transactional
    @Modifying
    @Query("delete from AlertDeliveryEntity d where d.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
