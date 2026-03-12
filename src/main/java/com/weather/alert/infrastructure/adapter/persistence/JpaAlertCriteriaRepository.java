package com.weather.alert.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface JpaAlertCriteriaRepository extends JpaRepository<AlertCriteriaEntity, String> {
    List<AlertCriteriaEntity> findByUserIdOrderByCreatedAtAscIdAsc(String userId);
    List<AlertCriteriaEntity> findByEnabled(Boolean enabled);

    @Transactional
    @Modifying
    @Query("delete from AlertCriteriaEntity c where c.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}
