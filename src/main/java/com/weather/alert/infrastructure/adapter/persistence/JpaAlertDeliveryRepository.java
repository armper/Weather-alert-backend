package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaAlertDeliveryRepository extends JpaRepository<AlertDeliveryEntity, String> {

    Optional<AlertDeliveryEntity> findByAlertIdAndChannel(String alertId, String channel);
}
