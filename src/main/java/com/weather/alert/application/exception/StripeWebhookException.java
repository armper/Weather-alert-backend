package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class StripeWebhookException extends ApiException {

    public StripeWebhookException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "STRIPE_WEBHOOK_ERROR", message, cause);
    }
}
