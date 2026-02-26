package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.NotificationChannel;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

final class NotificationChannelCodec {

    private NotificationChannelCodec() {
    }

    static String encode(List<NotificationChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            return "";
        }
        return channels.stream()
                .filter(Objects::nonNull)
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    static List<NotificationChannel> decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return List.of();
        }
        return Arrays.stream(encoded.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> NotificationChannel.valueOf(value.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }
}
