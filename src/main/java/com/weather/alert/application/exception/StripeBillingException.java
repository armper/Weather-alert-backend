package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class StripeBillingException extends ApiException {

    public StripeBillingException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "STRIPE_BILLING_ERROR", message, cause);
    }
}
