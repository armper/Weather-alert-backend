package com.weather.alert.infrastructure.web.controller;

import com.weather.alert.application.dto.NwsProductResponse;
import com.weather.alert.application.dto.PagedResponse;
import com.weather.alert.application.dto.WeatherDataResponse;
import com.weather.alert.domain.model.HydrologyQuery;
import com.weather.alert.domain.model.NwsProduct;
import com.weather.alert.domain.model.PagedResult;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.model.WeatherPointMetadata;
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
    @Operation(summary = "Get paginated active weather alerts from the indexed read model")
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

    @GetMapping("/hydrology/current")
    @Operation(summary = "Get latest NWPS observed river conditions for a gauge or nearby coordinate")
    public ResponseEntity<WeatherDataResponse> getHydrologyCurrentConditions(
            @Parameter(example = "28.5383") @RequestParam(required = false) Double latitude,
            @Parameter(example = "-81.3792") @RequestParam(required = false) Double longitude,
            @Parameter(example = "80") @RequestParam(required = false) @Min(1) @Max(500) Integer radiusKm,
            @Parameter(example = "ABNG1") @RequestParam(required = false) String gaugeId) {
        Optional<WeatherData> current = weatherDataPort.fetchHydrologyCurrentConditions(HydrologyQuery.builder()
                .latitude(latitude)
                .longitude(longitude)
                .searchRadiusKm(radiusKm == null ? null : radiusKm.doubleValue())
                .gaugeId(gaugeId)
                .build());
        return current.map(weatherData -> ResponseEntity.ok(toResponse(weatherData)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/hydrology/forecast")
    @Operation(summary = "Get latest NWPS forecast river conditions for a gauge or nearby coordinate")
    public ResponseEntity<WeatherDataResponse> getHydrologyForecastConditions(
            @Parameter(example = "28.5383") @RequestParam(required = false) Double latitude,
            @Parameter(example = "-81.3792") @RequestParam(required = false) Double longitude,
            @Parameter(example = "80") @RequestParam(required = false) @Min(1) @Max(500) Integer radiusKm,
            @Parameter(example = "ABNG1") @RequestParam(required = false) String gaugeId) {
        Optional<WeatherData> forecast = weatherDataPort.fetchHydrologyForecastConditions(HydrologyQuery.builder()
                .latitude(latitude)
                .longitude(longitude)
                .searchRadiusKm(radiusKm == null ? null : radiusKm.doubleValue())
                .gaugeId(gaugeId)
                .build());
        return forecast.map(weatherData -> ResponseEntity.ok(toResponse(weatherData)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search/location/{location}")
    @Operation(summary = "Search indexed weather alerts by location text")
    public ResponseEntity<List<WeatherDataResponse>> searchByLocation(
            @Parameter(example = "Seattle") @PathVariable String location,
            @Parameter(description = "Maximum number of results (max 200)", example = "50")
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        List<WeatherData> weatherData = weatherDataSearchPort.searchByLocation(location, limit);
        List<WeatherDataResponse> response = weatherData.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search/event/{eventType}")
    @Operation(summary = "Search indexed weather alerts by event type")
    public ResponseEntity<List<WeatherDataResponse>> searchByEventType(
            @Parameter(example = "Flood Warning") @PathVariable String eventType,
            @Parameter(description = "Maximum number of results (max 200)", example = "50")
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        List<WeatherData> weatherData = weatherDataSearchPort.searchByEventType(eventType, limit);
        List<WeatherDataResponse> response = weatherData.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conditions/history")
    @Operation(summary = "Get observation history for a coordinate to track trends")
    public ResponseEntity<List<WeatherDataResponse>> getObservationHistory(
            @Parameter(example = "47.6062") @RequestParam double latitude,
            @Parameter(example = "-122.3321") @RequestParam double longitude,
            @Parameter(description = "Number of hours of history (max 24)", example = "6")
            @RequestParam(defaultValue = "6") @Min(1) @Max(24) int hours) {
        List<WeatherData> weatherData = weatherDataPort.fetchObservationHistory(latitude, longitude, hours);
        List<WeatherDataResponse> response = weatherData.stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conditions/daily")
    @Operation(summary = "Get 7-day period forecast for a coordinate")
    public ResponseEntity<List<WeatherDataResponse>> getDailyForecast(
            @Parameter(example = "47.6062") @RequestParam double latitude,
            @Parameter(example = "-122.3321") @RequestParam double longitude) {
        List<WeatherData> weatherData = weatherDataPort.fetchDailyForecast(latitude, longitude);
        List<WeatherDataResponse> response = weatherData.stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/alerts/{alertId}")
    @Operation(summary = "Get a specific NOAA alert by its identifier")
    public ResponseEntity<WeatherDataResponse> getAlertById(
            @Parameter(example = "urn:oid:2.49.0.1.840.0.abc123") @PathVariable String alertId) {
        Optional<WeatherData> alert = weatherDataPort.fetchAlertById(alertId);
        return alert.map(a -> ResponseEntity.ok(toResponse(a)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/products")
    @Operation(summary = "List NWS text products by type and/or location")
    public ResponseEntity<List<NwsProductResponse>> getProducts(
            @Parameter(example = "AFD") @RequestParam(required = false) String type,
            @Parameter(example = "SEW") @RequestParam(required = false) String location,
            @Parameter(example = "28.5383") @RequestParam(required = false) Double latitude,
            @Parameter(example = "-81.3792") @RequestParam(required = false) Double longitude) {
        String resolvedLocation = resolveProductLocationCode(location, latitude, longitude);
        List<NwsProduct> products = weatherDataPort.fetchProductsByType(type, resolvedLocation);
        List<NwsProductResponse> response = products.stream().map(this::toProductResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get a specific NWS text product by its identifier")
    public ResponseEntity<NwsProductResponse> getProductById(
            @Parameter(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") @PathVariable String productId) {
        Optional<NwsProduct> product = weatherDataPort.fetchProductById(productId);
        return product.map(p -> ResponseEntity.ok(toProductResponse(p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/zones/{zoneType}/{zoneId}/forecast")
    @Operation(summary = "Get zone-based forecast from NWS")
    public ResponseEntity<List<WeatherDataResponse>> getZoneForecast(
            @Parameter(example = "forecast") @PathVariable String zoneType,
            @Parameter(example = "WAZ001") @PathVariable String zoneId) {
        List<WeatherData> weatherData = weatherDataPort.fetchZoneForecast(zoneType, zoneId);
        List<WeatherDataResponse> response = weatherData.stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/marine/{zoneId}/forecast")
    @Operation(summary = "Get marine zone forecast (convenience endpoint using zone forecast infrastructure)")
    public ResponseEntity<List<WeatherDataResponse>> getMarineForecast(
            @Parameter(example = "AMZ630") @PathVariable String zoneId) {
        List<WeatherData> weatherData = weatherDataPort.fetchZoneForecast("forecast", zoneId);
        List<WeatherDataResponse> response = weatherData.stream().map(this::toResponse).collect(Collectors.toList());
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
                .dewPoint(data.getDewPoint())
                .windGust(data.getWindGust())
                .skyCover(data.getSkyCover())
                .riverGaugeId(data.getRiverGaugeId())
                .riverObservedStage(data.getRiverObservedStage())
                .riverForecastStage(data.getRiverForecastStage())
                .riverFloodStage(data.getRiverFloodStage())
                .riverActionStage(data.getRiverActionStage())
                .riverObservedCategory(data.getRiverObservedCategory())
                .riverForecastCategory(data.getRiverForecastCategory())
                .riverStageUnit(data.getRiverStageUnit())
                .riverDistanceKm(data.getRiverDistanceKm())
                .apparentTemperature(data.getApparentTemperature())
                .windChill(data.getWindChill())
                .heatIndex(data.getHeatIndex())
                .visibility(data.getVisibility())
                .windDirection(data.getWindDirection())
                .snowfallAmount(data.getSnowfallAmount())
                .iceAccumulation(data.getIceAccumulation())
                .probabilityOfThunder(data.getProbabilityOfThunder())
                .ceilingHeight(data.getCeilingHeight())
                .timestamp(data.getTimestamp() != null ? data.getTimestamp().toString() : null)
                .build();
    }

    private NwsProductResponse toProductResponse(NwsProduct product) {
        return NwsProductResponse.builder()
                .id(product.getId())
                .wmoCollectiveId(product.getWmoCollectiveId())
                .issuingOffice(product.getIssuingOffice())
                .issuanceTime(product.getIssuanceTime() != null ? product.getIssuanceTime().toString() : null)
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .productText(product.getProductText())
                .build();
    }

    private String resolveProductLocationCode(String location, Double latitude, Double longitude) {
        if (location != null && !location.isBlank()) {
            return location.trim();
        }
        if (latitude == null || longitude == null) {
            return null;
        }
        Optional<WeatherPointMetadata> pointMetadata = weatherDataPort.fetchPointMetadata(latitude, longitude);
        return pointMetadata
                .map(WeatherPointMetadata::forecastOfficeId)
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);
    }
}
