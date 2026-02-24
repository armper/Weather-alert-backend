package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    protected ResourceNotFoundException(String errorCode, String message) {
        super(HttpStatus.NOT_FOUND, errorCode, message);
    }
}
