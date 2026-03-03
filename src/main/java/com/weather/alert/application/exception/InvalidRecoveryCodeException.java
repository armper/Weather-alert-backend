package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class InvalidRecoveryCodeException extends ApiException {

    public InvalidRecoveryCodeException() {
        super(HttpStatus.BAD_REQUEST, "INVALID_RECOVERY_CODE", "Invalid or expired recovery code");
    }
}
