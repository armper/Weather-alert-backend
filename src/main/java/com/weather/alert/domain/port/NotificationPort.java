package com.weather.alert.domain.port;

import com.weather.alert.domain.model.Alert;

/**
 * Port for dispatching alert notifications and follow-up delivery work.
 */
public interface NotificationPort {
    
    /**
     * Send an alert notification to a user
     */
    void sendAlert(Alert alert, String userId);
    
    /**
     * Publish an alert to downstream delivery and realtime channels.
     */
    void publishAlert(Alert alert);
}
