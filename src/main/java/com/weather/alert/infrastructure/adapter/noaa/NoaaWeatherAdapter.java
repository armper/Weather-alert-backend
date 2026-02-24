package com.weather.alert.infrastructure.adapter.noaa;

import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.WeatherDataPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Adapter for fetching weather data from NOAA API
 */
@Component
@Slf4j
public class NoaaWeatherAdapter implements WeatherDataPort {

    private final WebClient noaaWebClient;
    private final long requestTimeoutSeconds;
    private final long retryMaxAttempts;
    private final long retryBackoffMillis;

    public NoaaWeatherAdapter(
            WebClient noaaWebClient,
            @Value("${app.noaa.request-timeout-seconds:8}") long requestTimeoutSeconds,
            @Value("${app.noaa.retry-max-attempts:2}") long retryMaxAttempts,
            @Value("${app.noaa.retry-backoff-millis:250}") long retryBackoffMillis) {
        this.noaaWebClient = noaaWebClient;
        this.requestTimeoutSeconds = Math.max(1, requestTimeoutSeconds);
        this.retryMaxAttempts = Math.max(0, retryMaxAttempts);
        this.retryBackoffMillis = Math.max(50, retryBackoffMillis);
    }

    @Override
    public List<WeatherData> fetchActiveAlerts() {
        log.info("Fetching active alerts from NOAA API");
        return requestWithFallback(
                "active alerts",
                () -> noaaWebClient
                        .get()
                        .uri("/alerts/active")
                        .retrieve()
                        .bodyToMono(NoaaAlertResponse.class))
                .map(response -> response.getFeatures() == null ? List.<WeatherData>of() : mapToAlertWeatherData(response.getFeatures()))
                .orElseGet(List::of);
    }

    @Override
    public List<WeatherData> fetchAlertsForLocation(double latitude, double longitude) {
        log.info("Fetching alerts for location: {}, {}", latitude, longitude);
        return requestWithFallback(
                "alerts for location",
                () -> noaaWebClient
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/alerts/active")
                                .queryParam("point", latitude + "," + longitude)
                                .build())
                        .retrieve()
                        .bodyToMono(NoaaAlertResponse.class))
                .map(response -> response.getFeatures() == null ? List.<WeatherData>of() : mapToAlertWeatherData(response.getFeatures()))
                .orElseGet(List::of);
    }

    @Override
    public List<WeatherData> fetchAlertsForState(String stateCode) {
        log.info("Fetching alerts for state: {}", stateCode);
        return requestWithFallback(
                "alerts for state",
                () -> noaaWebClient
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/alerts/active")
                                .queryParam("area", stateCode)
                                .build())
                        .retrieve()
                        .bodyToMono(NoaaAlertResponse.class))
                .map(response -> response.getFeatures() == null ? List.<WeatherData>of() : mapToAlertWeatherData(response.getFeatures()))
                .orElseGet(List::of);
    }

    @Override
    public Optional<WeatherData> fetchCurrentConditions(double latitude, double longitude) {
        log.info("Fetching NOAA current conditions for: {}, {}", latitude, longitude);
        Optional<NoaaPointProperties> pointProperties = fetchPointProperties(latitude, longitude);
        if (pointProperties.isEmpty()) {
            return Optional.empty();
        }

        Optional<NoaaStationProperties> station = fetchPrimaryStation(pointProperties.get().getObservationStations());
        if (station.isEmpty() || station.get().getStationIdentifier() == null || station.get().getStationIdentifier().isBlank()) {
            log.warn("No observation station found for point {}, {}", latitude, longitude);
            return Optional.empty();
        }

        String stationId = station.get().getStationIdentifier();
        String stationName = station.get().getName();
        return requestWithFallback(
                "latest observation for station " + stationId,
                () -> noaaWebClient.get()
                        .uri("/stations/{stationId}/observations/latest", stationId)
                        .retrieve()
                        .bodyToMono(NoaaObservationResponse.class))
                .map(response -> mapObservationToWeatherData(response, stationId, stationName, latitude, longitude));
    }

    @Override
    public List<WeatherData> fetchForecastConditions(double latitude, double longitude, int forecastWindowHours) {
        int normalizedHours = Math.max(1, Math.min(forecastWindowHours, 168));
        log.info("Fetching NOAA forecast conditions for: {}, {} with {}h window", latitude, longitude, normalizedHours);
        Optional<NoaaPointProperties> pointProperties = fetchPointProperties(latitude, longitude);
        if (pointProperties.isEmpty() || pointProperties.get().getForecastHourly() == null) {
            return List.of();
        }

        return requestWithFallback(
                "hourly forecast",
                () -> noaaWebClient.get()
                        .uri(pointProperties.get().getForecastHourly())
                        .retrieve()
                        .bodyToMono(NoaaForecastHourlyResponse.class))
                .map(response -> mapForecastToWeatherData(response, latitude, longitude, normalizedHours))
                .orElseGet(List::of);
    }

    private Optional<NoaaPointProperties> fetchPointProperties(double latitude, double longitude) {
        return requestWithFallback(
                "point metadata",
                () -> noaaWebClient.get()
                        .uri("/points/{latitude},{longitude}", latitude, longitude)
                        .retrieve()
                        .bodyToMono(NoaaPointResponse.class))
                .map(NoaaPointResponse::getProperties);
    }

    private Optional<NoaaStationProperties> fetchPrimaryStation(String observationStationsUrl) {
        if (observationStationsUrl == null || observationStationsUrl.isBlank()) {
            return Optional.empty();
        }

        return requestWithFallback(
                "observation stations",
                () -> noaaWebClient.get()
                        .uri(observationStationsUrl)
                        .retrieve()
                        .bodyToMono(NoaaStationsResponse.class))
                .flatMap(response -> {
                    if (response.getFeatures() == null || response.getFeatures().isEmpty()) {
                        return Optional.empty();
                    }
                    return response.getFeatures().stream()
                            .map(NoaaStationFeature::getProperties)
                            .filter(properties -> properties != null && properties.getStationIdentifier() != null)
                            .findFirst();
                });
    }

    private List<WeatherData> mapToAlertWeatherData(List<NoaaAlertFeature> features) {
        return features.stream()
                .map(this::mapFeatureToWeatherData)
                .toList();
    }

    private WeatherData mapFeatureToWeatherData(NoaaAlertFeature feature) {
        NoaaAlertProperties props = feature.getProperties();

        return WeatherData.builder()
                .id(feature.getId() != null ? feature.getId() : UUID.randomUUID().toString())
                .location(props.getAreaDesc())
                .eventType(props.getEvent())
                .severity(props.getSeverity())
                .headline(props.getHeadline())
                .description(props.getDescription())
                .onset(parseInstantSafely(props.getOnset()))
                .expires(parseInstantSafely(props.getExpires()))
                .status(props.getStatus())
                .messageType(props.getMessageType())
                .category(props.getCategory())
                .urgency(props.getUrgency())
                .certainty(props.getCertainty())
                .timestamp(Instant.now())
                .build();
    }

    private WeatherData mapObservationToWeatherData(
            NoaaObservationResponse response,
            String stationId,
            String stationName,
            double latitude,
            double longitude) {
        NoaaObservationProperties props = response.getProperties();
        Instant observedAt = parseInstantSafely(props != null ? props.getTimestamp() : null);

        Double temperatureC = extractCelsius(props == null ? null : props.getTemperature());
        Double windKmh = extractWindKmh(props == null ? null : props.getWindSpeed());
        Double humidity = extractValue(props == null ? null : props.getRelativeHumidity());
        Double precipitationAmount = extractPrecipitationAmountMm(props == null ? null : props.getPrecipitationLastHour());

        String location = stationName != null && !stationName.isBlank() ? stationName : stationId;
        String headline = props != null && props.getTextDescription() != null ? props.getTextDescription() : "Current conditions";
        String description = "Latest NOAA observation from station " + stationId;

        return WeatherData.builder()
                .id("current-" + stationId + "-" + (observedAt != null ? observedAt.toEpochMilli() : UUID.randomUUID()))
                .location(location)
                .latitude(latitude)
                .longitude(longitude)
                .eventType("CURRENT_CONDITIONS")
                .headline(headline)
                .description(description)
                .status("CURRENT")
                .onset(observedAt)
                .temperature(temperatureC)
                .windSpeed(windKmh)
                .humidity(humidity)
                .precipitationAmount(precipitationAmount)
                .precipitation(precipitationAmount)
                .timestamp(observedAt != null ? observedAt : Instant.now())
                .build();
    }

    private List<WeatherData> mapForecastToWeatherData(
            NoaaForecastHourlyResponse response,
            double latitude,
            double longitude,
            int forecastWindowHours) {
        if (response.getProperties() == null || response.getProperties().getPeriods() == null) {
            return List.of();
        }

        Instant cutoff = Instant.now().plus(Duration.ofHours(forecastWindowHours));
        List<WeatherData> results = new ArrayList<>();
        for (NoaaForecastPeriod period : response.getProperties().getPeriods()) {
            Instant onset = parseInstantSafely(period.getStartTime());
            if (onset == null || onset.isAfter(cutoff)) {
                continue;
            }
            Instant expires = parseInstantSafely(period.getEndTime());

            Double tempC = NoaaUnitConversionUtils.toCelsius(
                    period.getTemperature() == null ? null : period.getTemperature().doubleValue(),
                    period.getTemperatureUnit());
            Double windKmh = NoaaUnitConversionUtils.parseWindSpeedToKmh(period.getWindSpeed());
            Double precipitationProbability = extractValue(period.getProbabilityOfPrecipitation());
            Double humidity = extractValue(period.getRelativeHumidity());

            WeatherData weatherData = WeatherData.builder()
                    .id("forecast-" + latitude + "-" + longitude + "-" + onset.toEpochMilli())
                    .location(String.format(Locale.US, "lat=%.4f,lon=%.4f", latitude, longitude))
                    .latitude(latitude)
                    .longitude(longitude)
                    .eventType("FORECAST_CONDITIONS")
                    .headline(period.getShortForecast())
                    .description(period.getDetailedForecast() != null ? period.getDetailedForecast() : period.getShortForecast())
                    .status("FORECAST")
                    .onset(onset)
                    .expires(expires)
                    .temperature(tempC)
                    .windSpeed(windKmh)
                    .humidity(humidity)
                    .precipitationProbability(precipitationProbability)
                    .precipitation(precipitationProbability)
                    .timestamp(Instant.now())
                    .build();
            results.add(weatherData);
        }
        return results;
    }

    private Double extractCelsius(NoaaQuantitativeValue value) {
        if (value == null) {
            return null;
        }
        return NoaaUnitConversionUtils.toCelsius(value.getValue(), value.getUnitCode());
    }

    private Double extractWindKmh(NoaaQuantitativeValue value) {
        if (value == null) {
            return null;
        }
        return NoaaUnitConversionUtils.toKilometersPerHour(value.getValue(), value.getUnitCode());
    }

    private Double extractPrecipitationAmountMm(NoaaQuantitativeValue value) {
        if (value == null) {
            return null;
        }
        return NoaaUnitConversionUtils.toMillimeters(value.getValue(), value.getUnitCode());
    }

    private Double extractValue(NoaaQuantitativeValue value) {
        return value == null ? null : value.getValue();
    }

    private Instant parseInstantSafely(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            log.warn("Unable to parse NOAA timestamp: {}", value);
            return null;
        }
    }

    private <T> Optional<T> requestWithFallback(String description, Supplier<Mono<T>> requestSupplier) {
        try {
            Mono<T> request = requestSupplier.get().timeout(Duration.ofSeconds(requestTimeoutSeconds));
            if (retryMaxAttempts > 0) {
                request = request.retryWhen(
                        Retry.backoff(retryMaxAttempts, Duration.ofMillis(retryBackoffMillis))
                                .filter(this::isRetryable)
                                .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
            }
            return Optional.ofNullable(request.block());
        } catch (Exception ex) {
            log.warn("NOAA request failed for {}. Falling back to empty result. cause={}", description, ex.getMessage());
            return Optional.empty();
        }
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientRequestException) {
            return true;
        }
        if (throwable instanceof WebClientResponseException responseException) {
            return responseException.getStatusCode().is5xxServerError() || responseException.getStatusCode().value() == 429;
        }
        String name = throwable.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        return name.contains("timeout");
    }
}
