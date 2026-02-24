package com.weather.alert.application.exception;

public class CriteriaNotFoundException extends ResourceNotFoundException {

    public CriteriaNotFoundException(String criteriaId) {
        super("CRITERIA_NOT_FOUND", "Criteria not found: " + criteriaId);
    }
}
