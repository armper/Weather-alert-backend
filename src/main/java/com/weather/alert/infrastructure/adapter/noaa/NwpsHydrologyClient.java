package com.weather.alert.infrastructure.adapter.noaa;

import com.weather.alert.domain.model.HydrologyQuery;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.WeatherFetchResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@Slf4j
public class NwpsHydrologyClient {

    private final WebClient nwpsWebClient;
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

    public NwpsHydrologyClient(
            @Qualifier("nwpsWebClient") WebClient nwpsWebClient,
            MeterRegistry meterRegistry,
            @Value("${app.nwps.request-timeout-seconds:${app.noaa.request-timeout-seconds:8}}") long requestTimeoutSeconds,
            @Value("${app.nwps.retry-max-attempts:${app.noaa.retry-max-attempts:2}}") long retryMaxAttempts,
            @Value("${app.nwps.retry-backoff-millis:${app.noaa.retry-backoff-millis:250}}") long retryBackoffMillis,
            @Value("${app.nwps.min-request-interval-millis:200}") long minRequestIntervalMillis,
            @Value("${app.nwps.outage-failure-threshold:4}") int outageFailureThreshold,
            @Value("${app.nwps.outage-open-seconds:30}") long outageOpenSeconds) {
        this.nwpsWebClient = nwpsWebClient;
        this.meterRegistry = meterRegistry;
        this.requestTimeoutSeconds = Math.max(1, requestTimeoutSeconds);
        this.retryMaxAttempts = Math.max(0, retryMaxAttempts);
        this.retryBackoffMillis = Math.max(50, retryBackoffMillis);
        this.minRequestIntervalMillis = Math.max(0, minRequestIntervalMillis);
        this.outageFailureThreshold = Math.max(1, outageFailureThreshold);
        this.outageOpenSeconds = Math.max(5, outageOpenSeconds);
    }

    public Optional<WeatherData> fetchCurrentConditions(HydrologyQuery query) {
        return fetchCurrentConditionsWithStatus(query).data();
    }

    public WeatherFetchResult<Optional<WeatherData>> fetchCurrentConditionsWithStatus(HydrologyQuery query) {
        RequestResult<NwpsGauge> gaugeResult = resolveGauge(query);
        if (!gaugeResult.successful()) {
            return WeatherFetchResult.failure(Optional.empty(), gaugeResult.failureReason());
        }
        WeatherData weatherData = mapGaugeToCurrentWeatherData(gaugeResult.payload(), query);
        return WeatherFetchResult.success(Optional.ofNullable(weatherData));
    }

    public Optional<WeatherData> fetchForecastConditions(HydrologyQuery query) {
        return fetchForecastConditionsWithStatus(query).data();
    }

    public WeatherFetchResult<Optional<WeatherData>> fetchForecastConditionsWithStatus(HydrologyQuery query) {
        RequestResult<NwpsGauge> gaugeResult = resolveGauge(query);
        if (!gaugeResult.successful()) {
            return WeatherFetchResult.failure(Optional.empty(), gaugeResult.failureReason());
        }
        WeatherData weatherData = mapGaugeToForecastWeatherData(gaugeResult.payload(), query);
        return WeatherFetchResult.success(Optional.ofNullable(weatherData));
    }

    private RequestResult<NwpsGauge> resolveGauge(HydrologyQuery query) {
        if (query == null) {
            return RequestResult.success(null);
        }

        String gaugeId = normalizeGaugeId(query.getGaugeId());
        if (gaugeId != null) {
            return fetchGaugeById(gaugeId);
        }

        if (query.getLatitude() == null || query.getLongitude() == null) {
            return RequestResult.success(null);
        }

        double radiusKm = normalizeSearchRadius(query.getSearchRadiusKm());
        BoundingBox bbox = boundingBox(query.getLatitude(), query.getLongitude(), radiusKm);

        RequestResult<NwpsGaugeListResponse> listResult = requestWithFallback(
                "nearby_gauges",
                () -> nwpsWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/gauges")
                                .queryParam("bbox.xmin", bbox.xmin())
                                .queryParam("bbox.ymin", bbox.ymin())
                                .queryParam("bbox.xmax", bbox.xmax())
                                .queryParam("bbox.ymax", bbox.ymax())
                                .queryParam("srid", "EPSG_4326")
                                .build())
                        .retrieve()
                        .bodyToMono(NwpsGaugeListResponse.class));
        if (!listResult.successful()) {
            return RequestResult.failure(listResult.failureReason());
        }

        List<NwpsGauge> gauges = listResult.payload() == null ? List.of() : listResult.payload().getGauges();
        if (gauges == null || gauges.isEmpty()) {
            return RequestResult.success(null);
        }

        Optional<NwpsGauge> nearest = gauges.stream()
                .filter(gauge -> gauge.getLatitude() != null && gauge.getLongitude() != null && gauge.getLid() != null)
                .min(Comparator.comparingDouble(gauge -> distanceKm(
                        query.getLatitude(),
                        query.getLongitude(),
                        gauge.getLatitude(),
                        gauge.getLongitude())));
        if (nearest.isEmpty()) {
            return RequestResult.success(null);
        }

        double nearestDistance = distanceKm(
                query.getLatitude(),
                query.getLongitude(),
                nearest.get().getLatitude(),
                nearest.get().getLongitude());
        if (nearestDistance > radiusKm) {
            return RequestResult.success(null);
        }

        return fetchGaugeById(nearest.get().getLid()).mapPayload(gauge -> {
            if (gauge == null) {
                return null;
            }
            gauge.setLatitude(gauge.getLatitude() == null ? nearest.get().getLatitude() : gauge.getLatitude());
            gauge.setLongitude(gauge.getLongitude() == null ? nearest.get().getLongitude() : gauge.getLongitude());
            return gauge;
        });
    }

    private RequestResult<NwpsGauge> fetchGaugeById(String gaugeId) {
        return requestWithFallback(
                "gauge_detail",
                () -> nwpsWebClient.get()
                        .uri("/gauges/{identifier}", gaugeId)
                        .retrieve()
                        .bodyToMono(NwpsGauge.class));
    }

    private WeatherData mapGaugeToCurrentWeatherData(NwpsGauge gauge, HydrologyQuery query) {
        if (gauge == null || gauge.getStatus() == null || gauge.getStatus().getObserved() == null) {
            return null;
        }
        NwpsGauge.StatusValue observed = gauge.getStatus().getObserved();
        Double observedStage = sanitizeGaugeValue(observed.getPrimary());
        Instant validTime = parseInstantSafely(observed.getValidTime());
        if (observedStage == null || validTime == null) {
            return null;
        }

        return WeatherData.builder()
                .id("river-current-" + safeGaugeId(gauge) + "-" + validTime.toEpochMilli())
                .location(resolveGaugeLocation(gauge))
                .latitude(gauge.getLatitude())
                .longitude(gauge.getLongitude())
                .eventType("RIVER_CURRENT_CONDITIONS")
                .headline(buildHeadline(gauge, "Observed river conditions"))
                .description(buildDescription(gauge, observedStage, observed.getFloodCategory(), "observed"))
                .status("NWPS_CURRENT")
                .onset(validTime)
                .riverGaugeId(safeGaugeId(gauge))
                .riverObservedStage(observedStage)
                .riverForecastStage(sanitizeGaugeValue(statusValue(gauge, false).getPrimary()))
                .riverFloodStage(minorFloodStage(gauge))
                .riverActionStage(actionFloodStage(gauge))
                .riverObservedCategory(formatFloodCategory(observed.getFloodCategory()))
                .riverForecastCategory(formatFloodCategory(statusValue(gauge, false).getFloodCategory()))
                .riverStageUnit(resolveStageUnit(gauge, observed))
                .riverDistanceKm(resolveDistanceKm(gauge, query))
                .timestamp(validTime)
                .build();
    }

    private WeatherData mapGaugeToForecastWeatherData(NwpsGauge gauge, HydrologyQuery query) {
        if (gauge == null || gauge.getStatus() == null || gauge.getStatus().getForecast() == null) {
            return null;
        }
        NwpsGauge.StatusValue forecast = gauge.getStatus().getForecast();
        Double forecastStage = sanitizeGaugeValue(forecast.getPrimary());
        Instant validTime = parseInstantSafely(forecast.getValidTime());
        if (forecastStage == null || validTime == null) {
            return null;
        }

        return WeatherData.builder()
                .id("river-forecast-" + safeGaugeId(gauge) + "-" + validTime.toEpochMilli())
                .location(resolveGaugeLocation(gauge))
                .latitude(gauge.getLatitude())
                .longitude(gauge.getLongitude())
                .eventType("RIVER_FORECAST_CONDITIONS")
                .headline(buildHeadline(gauge, "Forecast river conditions"))
                .description(buildDescription(gauge, forecastStage, forecast.getFloodCategory(), "forecast"))
                .status("NWPS_FORECAST")
                .onset(validTime)
                .riverGaugeId(safeGaugeId(gauge))
                .riverObservedStage(sanitizeGaugeValue(statusValue(gauge, true).getPrimary()))
                .riverForecastStage(forecastStage)
                .riverFloodStage(minorFloodStage(gauge))
                .riverActionStage(actionFloodStage(gauge))
                .riverObservedCategory(formatFloodCategory(statusValue(gauge, true).getFloodCategory()))
                .riverForecastCategory(formatFloodCategory(forecast.getFloodCategory()))
                .riverStageUnit(resolveStageUnit(gauge, forecast))
                .riverDistanceKm(resolveDistanceKm(gauge, query))
                .timestamp(validTime)
                .build();
    }

    private NwpsGauge.StatusValue statusValue(NwpsGauge gauge, boolean observed) {
        if (gauge == null || gauge.getStatus() == null) {
            return new NwpsGauge.StatusValue();
        }
        NwpsGauge.StatusValue value = observed ? gauge.getStatus().getObserved() : gauge.getStatus().getForecast();
        return value == null ? new NwpsGauge.StatusValue() : value;
    }

    private String buildHeadline(NwpsGauge gauge, String prefix) {
        return prefix + " for " + resolveGaugeLocation(gauge);
    }

    private String buildDescription(NwpsGauge gauge, Double stage, String category, String mode) {
        String unit = resolveStageUnitWithFallback(gauge, "ft");
        String floodCategory = formatFloodCategory(category);
        Double floodStage = minorFloodStage(gauge);
        return "%s stage %.2f %s at %s%s"
                .formatted(
                        capitalize(mode),
                        stage,
                        unit,
                        safeText(gauge == null ? null : gauge.getName(), "NWPS gauge"),
                        floodStage == null ? "" : " (flood stage %.2f %s)".formatted(floodStage, unit))
                + (floodCategory == null ? "" : ", category=" + floodCategory);
    }

    private String resolveGaugeLocation(NwpsGauge gauge) {
        if (gauge == null) {
            return "NWPS gauge";
        }
        String state = gauge.getState() == null ? null : gauge.getState().getAbbreviation();
        if (state == null || state.isBlank()) {
            return safeText(gauge.getName(), safeGaugeId(gauge));
        }
        return safeText(gauge.getName(), safeGaugeId(gauge)) + ", " + state;
    }

    private Double resolveDistanceKm(NwpsGauge gauge, HydrologyQuery query) {
        if (gauge == null || query == null || query.getLatitude() == null || query.getLongitude() == null
                || gauge.getLatitude() == null || gauge.getLongitude() == null) {
            return null;
        }
        return distanceKm(query.getLatitude(), query.getLongitude(), gauge.getLatitude(), gauge.getLongitude());
    }

    private String resolveStageUnit(NwpsGauge gauge, NwpsGauge.StatusValue value) {
        if (value != null && value.getPrimaryUnit() != null && !value.getPrimaryUnit().isBlank()) {
            return value.getPrimaryUnit();
        }
        return resolveStageUnitWithFallback(gauge, null);
    }

    private String resolveStageUnitWithFallback(NwpsGauge gauge, String fallback) {
        if (gauge != null && gauge.getFlood() != null && gauge.getFlood().getStageUnits() != null
                && !gauge.getFlood().getStageUnits().isBlank()) {
            return gauge.getFlood().getStageUnits();
        }
        return fallback == null ? "ft" : fallback;
    }

    private Double minorFloodStage(NwpsGauge gauge) {
        return sanitizeGaugeValue(gauge == null || gauge.getFlood() == null || gauge.getFlood().getCategories() == null
                || gauge.getFlood().getCategories().getMinor() == null
                ? null
                : gauge.getFlood().getCategories().getMinor().getStage());
    }

    private Double actionFloodStage(NwpsGauge gauge) {
        return sanitizeGaugeValue(gauge == null || gauge.getFlood() == null || gauge.getFlood().getCategories() == null
                || gauge.getFlood().getCategories().getAction() == null
                ? null
                : gauge.getFlood().getCategories().getAction().getStage());
    }

    private Double sanitizeGaugeValue(Double value) {
        return value == null || value <= -900.0 ? null : value;
    }

    private String normalizeGaugeId(String gaugeId) {
        if (gaugeId == null || gaugeId.isBlank()) {
            return null;
        }
        return gaugeId.trim().toUpperCase(Locale.ROOT);
    }

    private String formatFloodCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }

    private double normalizeSearchRadius(Double radiusKm) {
        if (radiusKm == null) {
            return 80.0;
        }
        return Math.max(5.0, Math.min(radiusKm, 500.0));
    }

    private BoundingBox boundingBox(double latitude, double longitude, double radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lonDivider = Math.max(0.1, Math.cos(Math.toRadians(latitude)));
        double lonDelta = radiusKm / (111.0 * lonDivider);
        return new BoundingBox(
                longitude - lonDelta,
                latitude - latDelta,
                longitude + lonDelta,
                latitude + latDelta);
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private Instant parseInstantSafely(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Instant parsed = Instant.parse(value);
            return parsed.isBefore(Instant.parse("1900-01-01T00:00:00Z")) ? null : parsed;
        } catch (Exception ex) {
            log.warn("Unable to parse NWPS timestamp: {}", value);
            return null;
        }
    }

    private String safeGaugeId(NwpsGauge gauge) {
        return gauge == null || gauge.getLid() == null || gauge.getLid().isBlank()
                ? UUID.randomUUID().toString()
                : gauge.getLid();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private <T> RequestResult<T> requestWithFallback(String operation, Supplier<Mono<T>> requestSupplier) {
        if (isOutageOpen()) {
            String reason = "outage guard open until " + outageOpenUntil;
            meterRegistry.counter("weather.nwps.requests", "operation", operation, "outcome", "short_circuit").increment();
            log.warn("Skipping NWPS request for operation={}: {}", operation, reason);
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
            meterRegistry.counter("weather.nwps.requests", "operation", operation, "outcome", "success").increment();
            return RequestResult.success(payload);
        } catch (Exception ex) {
            markProviderFailure();
            meterRegistry.counter("weather.nwps.requests", "operation", operation, "outcome", "failure").increment();
            log.warn("NWPS request failed for operation={}. Falling back to empty result. cause={}", operation, ex.getMessage());
            return RequestResult.failure(ex.getMessage());
        } finally {
            sample.stop(meterRegistry.timer("weather.nwps.request.duration", "operation", operation));
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
        log.warn("NWPS outage guard opened for {} seconds (until {}).", outageOpenSeconds, until);
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

    private record BoundingBox(double xmin, double ymin, double xmax, double ymax) {
    }
}
