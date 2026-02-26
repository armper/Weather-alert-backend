package com.weather.alert.domain.model;

public enum AlertDeliveryStatus {
    PENDING,
    IN_PROGRESS,
    SENT,
    RETRY_SCHEDULED,
    FAILED
}
