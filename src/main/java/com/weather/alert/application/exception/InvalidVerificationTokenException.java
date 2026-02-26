package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class InvalidVerificationTokenException extends ApiException {

    public InvalidVerificationTokenException() {
        super(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_TOKEN", "Verification token is invalid");
    }
}
