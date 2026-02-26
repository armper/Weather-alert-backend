package com.weather.alert.application.exception;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(String userId) {
        super("USER_NOT_FOUND", "User not found: " + userId);
    }
}

