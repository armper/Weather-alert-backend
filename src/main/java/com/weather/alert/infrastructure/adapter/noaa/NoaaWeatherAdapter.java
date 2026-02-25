package com.weather.alert.infrastructure.adapter.noaa;

import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.WeatherDataPort;
import com.weather.alert.domain.port.WeatherFetchResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Adapter for fetching weather data from NOAA API
 */
@Component
@Slf4j
public class NoaaWeatherAdapter implements WeatherDataPort {

    private final WebClient noaaWebClient;
    private final MeterRegistry meterRegistry;
    private final long requestTimeoutSeconds;
    private final long retryMaxAttempts;
    private final long retryBackoffMillis;
    private final long minRequestIntervalMillis;
    private final int outageFailureThreshold;
    private final long outageOpenSeconds;

    private final Object requestPacingLock = new Object();
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile long lastRequestAtMillis = 0L;
    private volatile Instant outageOpenUntil;

    public NoaaWeatherAdapter(
            WebClient noaaWebClient,
            MeterRegistry meterRegistry,
            @Value("${app.noaa.request-timeout-seconds:8}") long requestTimeoutSeconds,
            @Value("${app.noaa.retry-max-attempts:2}") long retryMaxAttempts,
            @Value("${app.noaa.retry-backoff-millis:250}") long retryBackoffMillis,
            @Value("${app.noaa.min-request-interval-millis:150}") long minRequestIntervalMillis,
            @Value("${app.noaa.outage-failure-threshold:4}") int outageFailureThreshold,
            @Value("${app.noaa.outage-open-seconds:30}") long outageOpenSeconds) {
        this.noaaWebClient = noaaWebClient;
        this.meterRegistry = meterRegistry;
        this.requestTimeoutSeconds = Math.max(1, requestTimeoutSeconds);
        this.retryMaxAttempts = Math.max(0, retryMaxAttempts);
        this.retryBackoffMillis = Math.max(50, retryBackoffMillis);
        this.minRequestIntervalMillis = Math.max(0, minRequestIntervalMillis);
        this.outageFailureThreshold = Math.max(1, outageFailureThreshold);
        this.outageOpenSeconds = Math.max(5, outageOpenSeconds);
    }

    @Override
    public List<WeatherData> fetchActiveAlerts() {
        return fetchActiveAlertsWithStatus().data();
    }

    @Override
    public WeatherFetchResult<List<WeatherData>> fetchActiveAlertsWithStatus() {
        log.info("Fetching active alerts from NOAA API");
        RequestResult<NoaaAlertResponse> response = requestWithFallback(
                "active_alerts",
                () -> noaaWebClient
                        .get()
                        .uri("/alerts/active")
                        .retrieve()
                        .bodyToMono(NoaaAlertResponse.class));
        if (!response.successful()) {
            return WeatherFetchResult.failure(List.of(), response.failureReason());
        }
        List<WeatherData> weatherData = response.payload() == null || response.payload().getFeatures() == null
                ? List.of()
                : mapToAlertWeatherData(response.payload().getFeatures());
        return WeatherFetchResult.success(weatherData);
    }

    @Override
    public List<WeatherData> fetchAlertsForLocation(double latitude, double longitude) {
        log.info("Fetching alerts for location: {}, {}", latitude, longitude);
        RequestResult<NoaaAlertResponse> response = requestWithFallback(
                "alerts_for_location",
                () -> noaaWebClient
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/alerts/active")
                                .queryParam("point", latitude + "," + longitude)
                                .build())
                        .retrieve()
                        .bodyToMono(NoaaAlertResponse.class));
        if (!response.successful() || response.payload() == null || response.payload().getFeatures() == null) {
            return List.of();
        }
        return mapToAlertWeatherData(response.payload().getFeatures());
    }

    @Override
    public List<WeatherData> fetchAlertsForState(String stateCode) {
        log.info("Fetching alerts for state: {}", stateCode);
        RequestResult<NoaaAlertResponse> response = requestWithFallback(
                "alerts_for_state",
                () -> noaaWebClient
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/alerts/active")
                                .queryParam("area", stateCode)
                                .build())
                        .retrieve()
                        .bodyToMono(NoaaAlertResponse.class));
        if (!response.successful() || response.payload() == null || response.payload().getFeatures() == null) {
            return List.of();
        }
        return mapToAlertWeatherData(response.payload().getFeatures());
    }

    @Override
    public Optional<WeatherData> fetchCurrentConditions(double latitude, double longitude) {
        return fetchCurrentConditionsWithStatus(latitude, longitude).data();
    }

    @Override
    public WeatherFetchResult<Optional<WeatherData>> fetchCurrentConditionsWithStatus(double latitude, double longitude) {
        log.info("Fetching NOAA current conditions for: {}, {}", latitude, longitude);
        RequestResult<NoaaPointProperties> pointPropertiesResult = fetchPointProperties(latitude, longitude);
        if (!pointPropertiesResult.successful()) {
            return WeatherFetchResult.failure(Optional.empty(), pointPropertiesResult.failureReason());
        }
        NoaaPointProperties pointProperties = pointPropertiesResult.payload();
        if (pointProperties == null) {
            return WeatherFetchResult.success(Optional.empty());
        }

        RequestResult<NoaaStationProperties> stationResult = fetchPrimaryStation(pointProperties.getObservationStations());
        if (!stationResult.successful()) {
            return WeatherFetchResult.failure(Optional.empty(), stationResult.failureReason());
        }
        NoaaStationProperties station = stationResult.payload();
        if (station == null || station.getStationIdentifier() == null || station.getStationIdentifier().isBlank()) {
            log.warn("No observation station found for point {}, {}", latitude, longitude);
            return WeatherFetchResult.success(Optional.empty());
        }

        String stationId = station.getStationIdentifier();
        String stationName = station.getName();
        RequestResult<NoaaObservationResponse> observationResult = requestWithFallback(
                "latest_observation",
                () -> noaaWebClient.get()
                        .uri("/stations/{stationId}/observations/latest", stationId)
                        .retrieve()
                        .bodyToMono(NoaaObservationResponse.class));
        if (!observationResult.successful()) {
            return WeatherFetchResult.failure(Optional.empty(), observationResult.failureReason());
        }
        WeatherData weatherData = mapObservationToWeatherData(observationResult.payload(), stationId, stationName, latitude, longitude);
        return WeatherFetchResult.success(Optional.ofNullable(weatherData));
    }

    @Override
    public List<WeatherData> fetchForecastConditions(double latitude, double longitude, int forecastWindowHours) {
        return fetchForecastConditionsWithStatus(latitude, longitude, forecastWindowHours).data();
    }

    @Override
    public WeatherFetchResult<List<WeatherData>> fetchForecastConditionsWithStatus(
            double latitude,
            double longitude,
            int forecastWindowHours) {
        int normalizedHours = Math.max(1, Math.min(forecastWindowHours, 168));
        log.info("Fetching NOAA forecast conditions for: {}, {} with {}h window", latitude, longitude, normalizedHours);

        RequestResult<NoaaPointProperties> pointPropertiesResult = fetchPointProperties(latitude, longitude);
        if (!pointPropertiesResult.successful()) {
            return WeatherFetchResult.failure(List.of(), pointPropertiesResult.failureReason());
        }
        NoaaPointProperties pointProperties = pointPropertiesResult.payload();
        if (pointProperties == null || pointProperties.getForecastHourly() == null) {
            return WeatherFetchResult.success(List.of());
        }

        RequestResult<NoaaForecastHourlyResponse> forecastResult = requestWithFallback(
                "hourly_forecast",
                () -> noaaWebClient.get()
                        .uri(pointProperties.getForecastHourly())
                        .retrieve()
                        .bodyToMono(NoaaForecastHourlyResponse.class));
        if (!forecastResult.successful()) {
            return WeatherFetchResult.failure(List.of(), forecastResult.failureReason());
        }
        return WeatherFetchResult.success(mapForecastToWeatherData(forecastResult.payload(), latitude, longitude, normalizedHours));
    }

    private RequestResult<NoaaPointProperties> fetchPointProperties(double latitude, double longitude) {
        return requestWithFallback(
                "point_metadata",
                () -> noaaWebClient.get()
                        .uri("/points/{latitude},{longitude}", latitude, longitude)
                        .retrieve()
                        .bodyToMono(NoaaPointResponse.class))
                .mapPayload(response -> response == null ? null : response.getProperties());
    }

    private RequestResult<NoaaStationProperties> fetchPrimaryStation(String observationStationsUrl) {
        if (observationStationsUrl == null || observationStationsUrl.isBlank()) {
            return RequestResult.success(null);
        }
        return requestWithFallback(
                "observation_stations",
                () -> noaaWebClient.get()
                        .uri(observationStationsUrl)
                        .retrieve()
                        .bodyToMono(NoaaStationsResponse.class))
                .mapPayload(response -> {
                    if (response == null || response.getFeatures() == null || response.getFeatures().isEmpty()) {
                        return null;
                    }
                    return response.getFeatures().stream()
                            .map(NoaaStationFeature::getProperties)
                            .filter(properties -> properties != null && properties.getStationIdentifier() != null)
                            .findFirst()
                            .orElse(null);
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
        if (response == null) {
            return null;
        }
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
        if (response == null || response.getProperties() == null || response.getProperties().getPeriods() == null) {
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

    private <T> RequestResult<T> requestWithFallback(String operation, Supplier<Mono<T>> requestSupplier) {
        if (isOutageOpen()) {
            String reason = "outage guard open until " + outageOpenUntil;
            meterRegistry.counter("weather.noaa.requests", "operation", operation, "outcome", "short_circuit").increment();
            log.warn("Skipping NOAA request for operation={}: {}", operation, reason);
            return RequestResult.failure(reason);
        }

        paceRequests();
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Mono<T> request = requestSupplier.get().timeout(Duration.ofSeconds(requestTimeoutSeconds));
            if (retryMaxAttempts > 0) {
                request = request.retryWhen(
                        Retry.backoff(retryMaxAttempts, Duration.ofMillis(retryBackoffMillis))
                                .filter(this::isRetryable)
                                .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
            }
            T payload = request.block();
            markProviderSuccess();
            meterRegistry.counter("weather.noaa.requests", "operation", operation, "outcome", "success").increment();
            return RequestResult.success(payload);
        } catch (Exception ex) {
            markProviderFailure();
            meterRegistry.counter("weather.noaa.requests", "operation", operation, "outcome", "failure").increment();
            log.warn("NOAA request failed for operation={}. Falling back to empty result. cause={}", operation, ex.getMessage());
            return RequestResult.failure(ex.getMessage());
        } finally {
            sample.stop(meterRegistry.timer("weather.noaa.request.duration", "operation", operation));
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

    private boolean isOutageOpen() {
        Instant until = outageOpenUntil;
        return until != null && until.isAfter(Instant.now());
    }

    private void markProviderSuccess() {
        consecutiveFailures.set(0);
        outageOpenUntil = null;
    }

    private void markProviderFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures < outageFailureThreshold) {
            return;
        }
        Instant until = Instant.now().plusSeconds(outageOpenSeconds);
        outageOpenUntil = until;
        consecutiveFailures.set(0);
        log.warn("NOAA outage guard opened for {} seconds (until {}).", outageOpenSeconds, until);
    }

    private void paceRequests() {
        if (minRequestIntervalMillis <= 0) {
            return;
        }
        synchronized (requestPacingLock) {
            long now = System.currentTimeMillis();
            long earliestNext = lastRequestAtMillis + minRequestIntervalMillis;
            long sleepMillis = earliestNext - now;
            if (sleepMillis > 0) {
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            lastRequestAtMillis = System.currentTimeMillis();
        }
    }

    private record RequestResult<T>(boolean successful, T payload, String failureReason) {
        static <T> RequestResult<T> success(T payload) {
            return new RequestResult<>(true, payload, null);
        }

        static <T> RequestResult<T> failure(String failureReason) {
            return new RequestResult<>(false, null, failureReason);
        }

        <R> RequestResult<R> mapPayload(Function<T, R> mapper) {
            if (!successful) {
                return RequestResult.failure(failureReason);
            }
            return RequestResult.success(mapper.apply(payload));
        }
    }
}
