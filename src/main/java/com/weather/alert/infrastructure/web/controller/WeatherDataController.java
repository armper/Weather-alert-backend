package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.PagedResponse;
import com.weather.alert.application.dto.WeatherDataResponse;
import com.weather.alert.domain.model.PagedResult;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.WeatherDataPort;
import com.weather.alert.domain.port.WeatherDataSearchPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for weather data
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Validated
@Tag(name = "Weather Data", description = "NOAA weather alert retrieval and search")
public class WeatherDataController {
    
    private final WeatherDataPort weatherDataPort;
    private final WeatherDataSearchPort weatherDataSearchPort;
    
    @GetMapping("/active")
    @Operation(summary = "Get paginated active weather alerts from Elasticsearch read model")
    public ResponseEntity<PagedResponse<WeatherDataResponse>> getActiveAlerts(
            @Parameter(description = "Zero-based page index", example = "0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (max 200)", example = "50") @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        PagedResult<WeatherData> pagedResults = weatherDataSearchPort.getActiveWeatherData(page, size);
        List<WeatherDataResponse> responseItems = pagedResults.getItems().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        PagedResponse<WeatherDataResponse> response = PagedResponse.<WeatherDataResponse>builder()
                .items(responseItems)
                .page(pagedResults.getPage())
                .size(pagedResults.getSize())
                .totalElements(pagedResults.getTotalElements())
                .totalPages(pagedResults.getTotalPages())
                .hasNext(pagedResults.isHasNext())
                .hasPrevious(pagedResults.isHasPrevious())
                .build();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/location")
    @Operation(summary = "Get alerts for a coordinate")
    public ResponseEntity<List<WeatherDataResponse>> getAlertsForLocation(
            @Parameter(example = "47.6062") @RequestParam double latitude,
            @Parameter(example = "-122.3321") @RequestParam double longitude) {
        List<WeatherData> weatherData = weatherDataPort.fetchAlertsForLocation(latitude, longitude);
        weatherData.forEach(weatherDataSearchPort::indexWeatherData);
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
        weatherData.forEach(weatherDataSearchPort::indexWeatherData);
        List<WeatherDataResponse> response = weatherData.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conditions/current")
    @Operation(summary = "Get latest NOAA current conditions for a coordinate")
    public ResponseEntity<WeatherDataResponse> getCurrentConditions(
            @Parameter(example = "28.5383") @RequestParam double latitude,
            @Parameter(example = "-81.3792") @RequestParam double longitude) {
        Optional<WeatherData> current = weatherDataPort.fetchCurrentConditions(latitude, longitude);
        return current.map(weatherData -> ResponseEntity.ok(toResponse(weatherData)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/conditions/forecast")
    @Operation(summary = "Get NOAA hourly forecast conditions for a coordinate")
    public ResponseEntity<List<WeatherDataResponse>> getForecastConditions(
            @Parameter(example = "28.5383") @RequestParam double latitude,
            @Parameter(example = "-81.3792") @RequestParam double longitude,
            @Parameter(description = "Forecast horizon in hours (max 168)", example = "48")
            @RequestParam(defaultValue = "48") @Min(1) @Max(168) int hours) {
        List<WeatherData> weatherData = weatherDataPort.fetchForecastConditions(latitude, longitude, hours);
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
                .temperature(data.getTemperature())
                .windSpeed(data.getWindSpeed())
                .precipitationProbability(data.getPrecipitationProbability())
                .precipitationAmount(data.getPrecipitationAmount())
                .humidity(data.getHumidity())
                .timestamp(data.getTimestamp() != null ? data.getTimestamp().toString() : null)
                .build();
    }
}
