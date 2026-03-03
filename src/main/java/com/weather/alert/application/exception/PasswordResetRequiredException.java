package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class PasswordResetRequiredException extends ApiException {

    public PasswordResetRequiredException(String userId) {
        super(HttpStatus.FORBIDDEN, "PASSWORD_RESET_REQUIRED",
                "Password reset required before sign-in: " + userId);
    }
}
