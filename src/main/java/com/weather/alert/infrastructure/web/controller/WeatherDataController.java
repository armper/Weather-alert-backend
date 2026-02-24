package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.WeatherDataResponse;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.WeatherDataPort;
import com.weather.alert.domain.port.WeatherDataSearchPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Weather Data", description = "NOAA weather alert retrieval and search")
public class WeatherDataController {
    
    private final WeatherDataPort weatherDataPort;
    private final WeatherDataSearchPort weatherDataSearchPort;
    
    @GetMapping("/active")
    @Operation(summary = "Get active weather alerts")
    public ResponseEntity<List<WeatherDataResponse>> getActiveAlerts() {
        List<WeatherData> weatherData = weatherDataPort.fetchActiveAlerts();
        List<WeatherDataResponse> response = weatherData.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/location")
    @Operation(summary = "Get alerts for a coordinate")
    public ResponseEntity<List<WeatherDataResponse>> getAlertsForLocation(
            @Parameter(example = "47.6062") @RequestParam double latitude,
            @Parameter(example = "-122.3321") @RequestParam double longitude) {
        List<WeatherData> weatherData = weatherDataPort.fetchAlertsForLocation(latitude, longitude);
        List<WeatherDataResponse> response = weatherData.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/state/{stateCode}")
    @Operation(summary = "Get alerts by US state code")
    public ResponseEntity<List<WeatherDataResponse>> getAlertsForState(
            @Parameter(example = "WA") @PathVariable String stateCode) {
        List<WeatherData> weatherData = weatherDataPort.fetchAlertsForState(stateCode);
        List<WeatherDataResponse> response = weatherData.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search/location/{location}")
    @Operation(summary = "Search indexed weather alerts by location text")
    public ResponseEntity<List<WeatherDataResponse>> searchByLocation(
            @Parameter(example = "Seattle") @PathVariable String location) {
        List<WeatherData> weatherData = weatherDataSearchPort.searchByLocation(location);
        List<WeatherDataResponse> response = weatherData.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search/event/{eventType}")
    @Operation(summary = "Search indexed weather alerts by event type")
    public ResponseEntity<List<WeatherDataResponse>> searchByEventType(
            @Parameter(example = "Flood Warning") @PathVariable String eventType) {
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
