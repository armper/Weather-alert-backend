package com.weather.alert.application.exception;

import com.weather.alert.domain.model.NotificationChannel;
import org.springframework.http.HttpStatus;

public class ChannelDestinationInUseException extends ApiException {

    public ChannelDestinationInUseException(NotificationChannel channel, String destination) {
        super(
                HttpStatus.CONFLICT,
                "CHANNEL_DESTINATION_IN_USE",
                "Destination is already used by another user for " + channel + ": " + destination);
    }
}
