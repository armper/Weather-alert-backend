package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface JpaTravelPlanRepository extends JpaRepository<TravelPlanEntity, String> {
    List<TravelPlanEntity> findByUserIdOrderByStartDateAscCreatedAtAsc(String userId);

    @Transactional
    @Modifying
    @Query("delete from TravelPlanEntity t where t.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}
