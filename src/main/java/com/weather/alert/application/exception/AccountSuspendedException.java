package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class AccountSuspendedException extends ApiException {

    public AccountSuspendedException(String userId) {
        super(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "Account is suspended: " + userId);
    }
}
