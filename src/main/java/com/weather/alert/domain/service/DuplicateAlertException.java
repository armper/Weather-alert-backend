package com.weather.alert.domain.service;

public class DuplicateAlertException extends RuntimeException {

    private final String criteriaId;
    private final String eventKey;

    public DuplicateAlertException(String criteriaId, String eventKey, Throwable cause) {
        super("Duplicate alert for criteriaId=" + criteriaId + " eventKey=" + eventKey, cause);
        this.criteriaId = criteriaId;
        this.eventKey = eventKey;
    }

    public String getCriteriaId() {
        return criteriaId;
    }

    public String getEventKey() {
        return eventKey;
    }
}
