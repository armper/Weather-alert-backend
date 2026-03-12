package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface JpaWeatherDataRepository extends JpaRepository<WeatherDataEntity, String> {

    Page<WeatherDataEntity> findAllByOrderByRecordedAtDesc(Pageable pageable);

    List<WeatherDataEntity> findByLocationContainingIgnoreCaseOrderByRecordedAtDesc(String location, Pageable pageable);

    List<WeatherDataEntity> findByEventTypeContainingIgnoreCaseOrderByRecordedAtDesc(String eventType, Pageable pageable);

    List<WeatherDataEntity> findBySeverityOrderByRecordedAtDesc(String severity, Pageable pageable);

    long deleteByRecordedAtBefore(Instant cutoff);
}
