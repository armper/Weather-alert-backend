package com.weather.alert.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AlertCriteriaTest {
    
    @Test
    void shouldMatchWeatherDataByEventType() {
        // Given
        AlertCriteria criteria = AlertCriteria.builder()
                .userId("user1")
                .eventType("Tornado")
                .enabled(true)
                .build();
        
        WeatherData weatherData = WeatherData.builder()
                .eventType("Tornado")
                .severity("SEVERE")
                .build();
        
        // When
        boolean matches = criteria.matches(weatherData);
        
        // Then
        assertTrue(matches);
    }
    
    @Test
    void shouldNotMatchWhenEventTypeDifferent() {
        // Given
        AlertCriteria criteria = AlertCriteria.builder()
                .userId("user1")
                .eventType("Tornado")
                .enabled(true)
                .build();
        
        WeatherData weatherData = WeatherData.builder()
                .eventType("Flood")
                .severity("SEVERE")
                .build();
        
        // When
        boolean matches = criteria.matches(weatherData);
        
        // Then
        assertFalse(matches);
    }
    
    @Test
    void shouldMatchBySeverity() {
        // Given
        AlertCriteria criteria = AlertCriteria.builder()
                .userId("user1")
                .minSeverity("MODERATE")
                .enabled(true)
                .build();
        
        WeatherData weatherData = WeatherData.builder()
                .eventType("Storm")
                .severity("SEVERE")
                .build();
        
        // When
        boolean matches = criteria.matches(weatherData);
        
        // Then
        assertTrue(matches);
    }
    
    @Test
    void shouldMatchByTemperatureThreshold() {
        // Given
        AlertCriteria criteria = AlertCriteria.builder()
                .userId("user1")
                .maxTemperature(35.0)
                .enabled(true)
                .build();
        
        WeatherData weatherData = WeatherData.builder()
                .temperature(40.0)
                .build();
        
        // When
        boolean matches = criteria.matches(weatherData);
        
        // Then
        assertTrue(matches);
    }
    
    @Test
    void shouldNotMatchWhenDisabled() {
        // Given
        AlertCriteria criteria = AlertCriteria.builder()
                .userId("user1")
                .eventType("Tornado")
                .enabled(false)
                .build();
        
        WeatherData weatherData = WeatherData.builder()
                .eventType("Tornado")
                .severity("SEVERE")
                .build();
        
        // When
        boolean matches = criteria.matches(weatherData);
        
        // Then
        assertFalse(matches);
    }
    
    @Test
    void shouldMatchByLocation() {
        // Given
        AlertCriteria criteria = AlertCriteria.builder()
                .userId("user1")
                .location("Seattle")
                .enabled(true)
                .build();
        
        WeatherData weatherData = WeatherData.builder()
                .location("Seattle, WA")
                .eventType("Storm")
                .build();
        
        // When
        boolean matches = criteria.matches(weatherData);
        
        // Then
        assertTrue(matches);
    }

    @Test
    void shouldNotMatchWhenThresholdCriterionIsNotMet() {
        // Given
        AlertCriteria criteria = AlertCriteria.builder()
                .userId("user1")
                .eventType("Tornado")
                .maxTemperature(35.0)
                .enabled(true)
                .build();

        WeatherData weatherData = WeatherData.builder()
                .eventType("Tornado")
                .temperature(30.0)
                .build();

        // When
        boolean matches = criteria.matches(weatherData);

        // Then
        assertFalse(matches);
    }

    @Test
    void shouldNotMatchWhenEnabledIsNull() {
        // Given
        AlertCriteria criteria = AlertCriteria.builder()
                .userId("user1")
                .eventType("Tornado")
                .build();

        WeatherData weatherData = WeatherData.builder()
                .eventType("Tornado")
                .build();

        // When
        boolean matches = criteria.matches(weatherData);

        // Then
        assertFalse(matches);
    }

    @Test
    void shouldNotMatchWhenWeatherDataIsNull() {
        // Given
        AlertCriteria criteria = AlertCriteria.builder()
                .userId("user1")
                .eventType("Tornado")
                .enabled(true)
                .build();

        // When
        boolean matches = criteria.matches(null);

        // Then
        assertFalse(matches);
    }
}
