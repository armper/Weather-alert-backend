package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class InvalidAlertTransitionException extends ApiException {

    public InvalidAlertTransitionException(String alertId, String fromState, String toState) {
        super(
                HttpStatus.CONFLICT,
                "INVALID_ALERT_TRANSITION",
                "Cannot transition alert %s from %s to %s".formatted(alertId, fromState, toState)
        );
    }
}
