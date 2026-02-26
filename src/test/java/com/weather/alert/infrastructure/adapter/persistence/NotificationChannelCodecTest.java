package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.NotificationChannel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationChannelCodecTest {

    @Test
    void shouldEncodeAndDecodeNotificationChannels() {
        String encoded = NotificationChannelCodec.encode(List.of(NotificationChannel.EMAIL, NotificationChannel.SMS));
        assertEquals("EMAIL,SMS", encoded);

        List<NotificationChannel> decoded = NotificationChannelCodec.decode(encoded);
        assertEquals(List.of(NotificationChannel.EMAIL, NotificationChannel.SMS), decoded);
    }

    @Test
    void shouldHandleEmptyValues() {
        assertEquals("", NotificationChannelCodec.encode(List.of()));
        assertTrue(NotificationChannelCodec.decode("").isEmpty());
    }
}
