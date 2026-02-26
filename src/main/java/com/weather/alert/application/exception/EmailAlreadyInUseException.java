package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyInUseException extends ApiException {

    public EmailAlreadyInUseException(String email) {
        super(HttpStatus.CONFLICT, "EMAIL_ALREADY_IN_USE", "Email is already in use: " + email);
    }
}

