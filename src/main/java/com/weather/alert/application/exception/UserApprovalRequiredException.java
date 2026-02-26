package com.weather.alert.application.exception;

import org.springframework.http.HttpStatus;

public class UserApprovalRequiredException extends ApiException {

    public UserApprovalRequiredException(String userId) {
        super(HttpStatus.FORBIDDEN, "USER_APPROVAL_REQUIRED",
                "User account is pending admin approval: " + userId);
    }
}

