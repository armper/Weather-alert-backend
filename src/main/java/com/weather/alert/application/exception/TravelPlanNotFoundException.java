package com.weather.alert.application.exception;

public class TravelPlanNotFoundException extends ResourceNotFoundException {

    public TravelPlanNotFoundException(String planId) {
        super("TRAVEL_PLAN_NOT_FOUND", "Travel plan not found: " + planId);
    }
}
