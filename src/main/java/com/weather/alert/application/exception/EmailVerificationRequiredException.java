package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class EmailVerificationRequiredException extends ApiException {

    public EmailVerificationRequiredException(String userId) {
        super(HttpStatus.FORBIDDEN, "EMAIL_VERIFICATION_REQUIRED",
                "User email must be verified before login: " + userId);
    }
}

