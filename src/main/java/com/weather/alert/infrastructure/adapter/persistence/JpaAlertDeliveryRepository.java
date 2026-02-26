package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaAlertDeliveryRepository extends JpaRepository<AlertDeliveryEntity, String> {

    Optional<AlertDeliveryEntity> findByAlertIdAndChannel(String alertId, String channel);

    List<AlertDeliveryEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            List<String> statuses,
            Instant nextAttemptAt,
            Pageable pageable);
}
