package com.weather.alert.application.exception;

public class ChannelVerificationNotFoundException extends ResourceNotFoundException {

    public ChannelVerificationNotFoundException(String verificationId) {
        super("CHANNEL_VERIFICATION_NOT_FOUND", "Channel verification not found: " + verificationId);
    }
}
