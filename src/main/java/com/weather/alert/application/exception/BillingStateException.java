package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class BillingStateException extends ApiException {

    public BillingStateException(String message) {
        super(HttpStatus.CONFLICT, "BILLING_STATE_ERROR", message);
    }
}
