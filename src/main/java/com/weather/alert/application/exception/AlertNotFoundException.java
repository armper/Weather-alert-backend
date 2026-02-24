package com.weather.alert.application.exception;

public class AlertNotFoundException extends ResourceNotFoundException {

    public AlertNotFoundException(String alertId) {
        super("ALERT_NOT_FOUND", "Alert not found: " + alertId);
    }
}
