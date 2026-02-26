package com.weather.alert.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.retention")
@Data
public class DataRetentionProperties {

    /**
     * Master switch for scheduled data cleanup.
     */
    private boolean enabled = true;

    /**
     * Retain generated alerts for this many days. 0 disables alert cleanup.
     */
    private long alertsDays = 2;

    /**
     * Retain indexed weather documents for this many hours. 0 disables weather cleanup.
     */
    private long weatherDataHours = 72;

    /**
     * Retain criteria anti-spam state rows for this many days. 0 disables age-based criteria-state cleanup.
     */
    private long criteriaStateDays = 14;

    /**
     * Remove criteria-state rows whose criteria no longer exists.
     */
    private boolean cleanupOrphanCriteriaState = true;
}
