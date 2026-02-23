package com.weather.alert.domain.port;

import com.weather.alert.domain.model.Alert;

/**
 * Port for sending notifications
 */
public interface NotificationPort {
    
    /**
     * Send an alert notification to a user
     */
    void sendAlert(Alert alert, String userId);
    
    /**
     * Publish alert to messaging system (Kafka)
     */
    void publishAlert(Alert alert);
}
