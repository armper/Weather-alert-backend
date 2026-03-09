package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class BillingNotConfiguredException extends ApiException {

    public BillingNotConfiguredException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "BILLING_NOT_CONFIGURED", "Stripe billing is not configured");
    }
}
