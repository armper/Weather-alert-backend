package com.weather.alert.application.dto;

import com.weather.alert.domain.model.AlertCriteria;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateAlertCriteriaRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void shouldFailWhenTemperatureDirectionProvidedWithoutThreshold() {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user-1")
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .build();

        Set<ConstraintViolation<CreateAlertCriteriaRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldFailWhenRainThresholdProvidedWithoutType() {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user-1")
                .rainThreshold(40.0)
                .build();

        Set<ConstraintViolation<CreateAlertCriteriaRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldPassWhenPairsAndMonitoringModesAreValid() {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user-1")
                .latitude(28.5383)
                .longitude(-81.3792)
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .rainThreshold(40.0)
                .rainThresholdType(AlertCriteria.RainThresholdType.PROBABILITY)
                .monitorCurrent(true)
                .monitorForecast(false)
                .build();

        Set<ConstraintViolation<CreateAlertCriteriaRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldFailWhenConditionThresholdsDoNotIncludeCoordinates() {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user-1")
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .build();

        Set<ConstraintViolation<CreateAlertCriteriaRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldFailWhenForecastWindowSetButMonitorForecastDisabled() {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user-1")
                .monitorCurrent(true)
                .monitorForecast(false)
                .forecastWindowHours(24)
                .build();

        Set<ConstraintViolation<CreateAlertCriteriaRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldFailWhenProbabilityRainThresholdExceeds100() {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .userId("user-1")
                .latitude(28.5383)
                .longitude(-81.3792)
                .rainThreshold(120.0)
                .rainThresholdType(AlertCriteria.RainThresholdType.PROBABILITY)
                .build();

        Set<ConstraintViolation<CreateAlertCriteriaRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldFailWhenThresholdTemperatureModeIsMixedWithLegacyRangeMode() {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .temperatureThreshold(93.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.ABOVE)
                .minTemperature(10.0)
                .build();

        Set<ConstraintViolation<CreateAlertCriteriaRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldFailWhenThresholdRainModeIsMixedWithLegacyPrecipitationMode() {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .latitude(28.5383)
                .longitude(-81.3792)
                .rainThreshold(60.0)
                .rainThresholdType(AlertCriteria.RainThresholdType.PROBABILITY)
                .maxPrecipitation(5.0)
                .build();

        Set<ConstraintViolation<CreateAlertCriteriaRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        CreateAlertCriteriaRequest request = CreateAlertCriteriaRequest.builder()
                .name("   ")
                .location("Orlando")
                .build();

        Set<ConstraintViolation<CreateAlertCriteriaRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
