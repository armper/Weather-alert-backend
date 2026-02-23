package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.WeatherDataResponse;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.WeatherDataPort;
import com.weather.alert.domain.port.WeatherDataSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for weather data
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherDataController {
    
    private final WeatherDataPort weatherDataPort;
    private final WeatherDataSearchPort weatherDataSearchPort;
    
    @GetMapping("/active")
    public ResponseEntity<List<WeatherDataResponse>> getActiveAlerts() {
        List<WeatherData> weatherData = weatherDataPort.fetchActiveAlerts();
        List<WeatherDataResponse> response = weatherData.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/location")
    public ResponseEntity<List<WeatherDataResponse>> getAlertsForLocation(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        List<WeatherData> weatherData = weatherDataPort.fetchAlertsForLocation(latitude, longitude);
        List<WeatherDataResponse> response = weatherData.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/state/{stateCode}")
    public ResponseEntity<List<WeatherDataResponse>> getAlertsForState(@PathVariable String stateCode) {
        List<WeatherData> weatherData = weatherDataPort.fetchAlertsForState(stateCode);
        List<WeatherDataResponse> response = weatherData.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search/location/{location}")
    public ResponseEntity<List<WeatherDataResponse>> searchByLocation(@PathVariable String location) {
        List<WeatherData> weatherData = weatherDataSearchPort.searchByLocation(location);
        List<WeatherDataResponse> response = weatherData.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search/event/{eventType}")
    public ResponseEntity<List<WeatherDataResponse>> searchByEventType(@PathVariable String eventType) {
        List<WeatherData> weatherData = weatherDataSearchPort.searchByEventType(eventType);
        List<WeatherDataResponse> response = weatherData.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    private WeatherDataResponse toResponse(WeatherData data) {
        return WeatherDataResponse.builder()
                .id(data.getId())
                .location(data.getLocation())
                .eventType(data.getEventType())
                .severity(data.getSeverity())
                .headline(data.getHeadline())
                .description(data.getDescription())
                .onset(data.getOnset() != null ? data.getOnset().toString() : null)
                .expires(data.getExpires() != null ? data.getExpires().toString() : null)
                .build();
    }
}
