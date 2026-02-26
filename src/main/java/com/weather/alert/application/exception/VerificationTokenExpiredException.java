package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class VerificationTokenExpiredException extends ApiException {

    public VerificationTokenExpiredException() {
        super(HttpStatus.BAD_REQUEST, "VERIFICATION_TOKEN_EXPIRED", "Verification token has expired");
    }
}
