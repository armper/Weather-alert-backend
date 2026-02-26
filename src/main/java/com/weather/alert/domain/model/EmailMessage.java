package com.weather.alert.domain.model;

import lombok.Builder;

@Builder
public record EmailMessage(
        String to,
        String subject,
        String body) {
}
