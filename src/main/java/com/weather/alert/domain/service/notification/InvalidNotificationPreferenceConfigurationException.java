package com.weather.alert.domain.service.notification;

public class InvalidNotificationPreferenceConfigurationException extends RuntimeException {

    public InvalidNotificationPreferenceConfigurationException(String message) {
        super(message);
    }
}
