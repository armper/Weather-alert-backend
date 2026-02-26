package com.weather.alert.domain.service.notification;

import com.weather.alert.domain.model.DeliveryFailureType;

public class EmailDeliveryException extends RuntimeException {

    private final DeliveryFailureType failureType;

    public EmailDeliveryException(DeliveryFailureType failureType, String message, Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
    }

    public DeliveryFailureType getFailureType() {
        return failureType;
    }
}
