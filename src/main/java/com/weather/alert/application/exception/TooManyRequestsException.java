package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {

    public TooManyRequestsException(String errorCode, String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, errorCode, message);
    }
}
