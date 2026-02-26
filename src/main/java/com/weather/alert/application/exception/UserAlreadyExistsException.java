package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ApiException {

    public UserAlreadyExistsException(String userId) {
        super(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "User already exists: " + userId);
    }
}

