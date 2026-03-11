package com.weather.alert.domain.model;

import lombok.Builder;

@Builder
public record SmsMessage(
        String to,
        String body) {
}
