package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class InvalidCurrentPasswordException extends ApiException {

    public InvalidCurrentPasswordException() {
        super(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "Current password is incorrect");
    }
}
