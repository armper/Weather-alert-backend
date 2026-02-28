package com.weather.alert.domain.service.evaluation;

import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.WeatherData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertCriteriaRuleEvaluatorTest {

    private final AlertCriteriaRuleEvaluator evaluator = new AlertCriteriaRuleEvaluator();

    @Test
    void shouldMatchTemperatureBelowThresholdInFahrenheit() {
        AlertCriteria criteria = AlertCriteria.builder()
                .enabled(true)
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .build();

        WeatherData weatherData = WeatherData.builder()
                .temperature(14.0) // ~57.2F
                .build();

        assertTrue(evaluator.matches(criteria, weatherData));
    }

    @Test
    void shouldMatchRainProbabilityThreshold() {
        AlertCriteria criteria = AlertCriteria.builder()
                .enabled(true)
                .rainThreshold(40.0)
                .rainThresholdType(AlertCriteria.RainThresholdType.PROBABILITY)
                .build();

        WeatherData forecast = WeatherData.builder()
                .precipitationProbability(65.0)
                .build();

        assertTrue(evaluator.matches(criteria, forecast));
    }

    @Test
    void shouldMatchRainAmountThreshold() {
        AlertCriteria criteria = AlertCriteria.builder()
                .enabled(true)
                .rainThreshold(2.0)
                .rainThresholdType(AlertCriteria.RainThresholdType.AMOUNT)
                .build();

        WeatherData observation = WeatherData.builder()
                .precipitationAmount(4.5)
                .build();

        assertTrue(evaluator.matches(criteria, observation));
    }

    @Test
    void shouldRequireConfiguredFiltersToPass() {
        AlertCriteria criteria = AlertCriteria.builder()
                .enabled(true)
                .location("Orlando")
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .build();

        WeatherData weatherData = WeatherData.builder()
                .location("Miami, FL")
                .temperature(12.0)
                .build();

        assertFalse(evaluator.matches(criteria, weatherData));
    }

    @Test
    void shouldMatchWhenAnyTriggerRuleMatches() {
        AlertCriteria criteria = AlertCriteria.builder()
                .enabled(true)
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .rainThreshold(50.0)
                .rainThresholdType(AlertCriteria.RainThresholdType.PROBABILITY)
                .build();

        WeatherData weatherData = WeatherData.builder()
                .temperature(20.0) // no temperature match (~68F)
                .precipitationProbability(70.0) // rain match
                .build();

        assertTrue(evaluator.matches(criteria, weatherData));
    }

    @Test
    void shouldNotMatchTemperatureAboveThresholdWhenCurrentTemperatureIsLower() {
        AlertCriteria criteria = AlertCriteria.builder()
                .enabled(true)
                .temperatureThreshold(93.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.ABOVE)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .build();

        WeatherData current = WeatherData.builder()
                .temperature(18.0) // 64.4F
                .build();

        assertFalse(evaluator.matches(criteria, current));
    }

    @Test
    void shouldNotMatchWhenLocationFilterPassesButWindThresholdDoesNot() {
        AlertCriteria criteria = AlertCriteria.builder()
                .enabled(true)
                .location("Orlando")
                .maxWindSpeed(70.0)
                .build();

        WeatherData current = WeatherData.builder()
                .location("Orlando Executive Airport")
                .windSpeed(20.0)
                .build();

        assertFalse(evaluator.matches(criteria, current));
    }

    @Test
    void shouldIgnoreLegacyTemperatureRangeWhenThresholdModeIsConfigured() {
        AlertCriteria criteria = AlertCriteria.builder()
                .enabled(true)
                .temperatureThreshold(93.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.ABOVE)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                // Legacy field is conflicting and should be ignored when threshold mode is used.
                .minTemperature(93.0)
                .build();

        WeatherData current = WeatherData.builder()
                .temperature(18.0) // 64.4F
                .build();

        assertFalse(evaluator.matches(criteria, current));
    }

    @Test
    void shouldMatchEventTypeAgainstConditionTextForCurrentAndForecastData() {
        AlertCriteria criteria = AlertCriteria.builder()
                .enabled(true)
                .eventType("Rain")
                .rainThreshold(40.0)
                .rainThresholdType(AlertCriteria.RainThresholdType.PROBABILITY)
                .build();

        WeatherData conditionData = WeatherData.builder()
                .eventType("FORECAST_CONDITIONS")
                .headline("Chance of Rain Showers")
                .description("Rain likely overnight")
                .precipitationProbability(80.0)
                .build();

        assertTrue(evaluator.matches(criteria, conditionData));
    }

    @Test
    void shouldReturnTrueWhenOnlyFilterRulesConfiguredAndTheyPass() {
        AlertCriteria criteria = AlertCriteria.builder()
                .enabled(true)
                .eventType("Tornado Warning")
                .minSeverity("SEVERE")
                .build();

        WeatherData weatherData = WeatherData.builder()
                .eventType("Tornado Warning")
                .severity("EXTREME")
                .build();

        assertTrue(evaluator.matches(criteria, weatherData));
    }

    @Test
    void shouldNotMatchDisabledCriteria() {
        AlertCriteria criteria = AlertCriteria.builder()
                .enabled(false)
                .eventType("Tornado")
                .build();

        WeatherData weatherData = WeatherData.builder()
                .eventType("Tornado")
                .build();

        assertFalse(evaluator.matches(criteria, weatherData));
    }
}
