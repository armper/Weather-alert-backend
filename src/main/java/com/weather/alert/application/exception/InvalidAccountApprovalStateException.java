package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class InvalidAccountApprovalStateException extends ApiException {

    public InvalidAccountApprovalStateException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT_APPROVAL_STATE", message);
    }
}

