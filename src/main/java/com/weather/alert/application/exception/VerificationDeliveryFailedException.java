package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class VerificationDeliveryFailedException extends ApiException {

    public VerificationDeliveryFailedException(String destination, Throwable cause) {
        super(
                HttpStatus.SERVICE_UNAVAILABLE,
                "VERIFICATION_DELIVERY_FAILED",
                "Failed to deliver verification email to: " + destination,
                cause);
    }
}
