package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class InvalidUserAccountStateException extends ApiException {

    public InvalidUserAccountStateException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_USER_ACCOUNT_STATE", message);
    }
}
