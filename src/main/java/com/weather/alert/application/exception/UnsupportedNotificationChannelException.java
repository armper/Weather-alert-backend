package com.weather.alert.application.exception;

import com.weather.alert.domain.model.NotificationChannel;
import org.springframework.http.HttpStatus;

public class UnsupportedNotificationChannelException extends ApiException {

    public UnsupportedNotificationChannelException(NotificationChannel channel) {
        super(
                HttpStatus.BAD_REQUEST,
                "UNSUPPORTED_NOTIFICATION_CHANNEL",
                "Channel " + channel + " is not supported for verification in this version");
    }
}
