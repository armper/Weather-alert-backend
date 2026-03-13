package com.weather.alert.infrastructure.adapter.noaa;

import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.model.HydrologyQuery;
import com.weather.alert.domain.model.NwsProduct;
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
import java.util.Objects;
import java.util.TreeMap;
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
    private final NwpsHydrologyClient nwpsHydrologyClient;
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
            NwpsHydrologyClient nwpsHydrologyClient,
            MeterRegistry meterRegistry,
            @Value("${app.noaa.request-timeout-seconds:8}") long requestTimeoutSeconds,
            @Value("${app.noaa.retry-max-attempts:2}") long retryMaxAttempts,
            @Value("${app.noaa.retry-backoff-millis:250}") long retryBackoffMillis,
            @Value("${app.noaa.min-request-interval-millis:150}") long minRequestIntervalMillis,
            @Value("${app.noaa.outage-failure-threshold:4}") int outageFailureThreshold,
            @Value("${app.noaa.outage-open-seconds:30}") long outageOpenSeconds) {
        this.noaaWebClient = noaaWebClient;
        this.nwpsHydrologyClient = nwpsHydrologyClient;
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
            if (pointProperties == null || pointProperties.getForecastGridData() == null) {
                return WeatherFetchResult.success(List.of());
            }
        }

        RequestResult<NoaaForecastHourlyResponse> forecastResult =
                fetchHourlyForecast(pointProperties.getForecastHourly());
        RequestResult<NoaaGridpointForecastResponse> gridForecastResult =
                fetchGridForecast(pointProperties.getForecastGridData());

        boolean hourlySuccessful = forecastResult.successful();
        boolean gridSuccessful = gridForecastResult.successful();
        if (!hourlySuccessful && !gridSuccessful) {
            String reason = joinFailureReasons(forecastResult.failureReason(), gridForecastResult.failureReason());
            return WeatherFetchResult.failure(List.of(), reason);
        }

        List<WeatherData> forecastData = hourlySuccessful
                ? mapForecastToWeatherData(forecastResult.payload(), latitude, longitude, normalizedHours)
                : List.of();
        if (!forecastData.isEmpty() && gridSuccessful) {
            enrichForecastWithGridData(forecastData, gridForecastResult.payload());
        }
        if (forecastData.isEmpty() && gridSuccessful) {
            forecastData = mapGridForecastToWeatherData(gridForecastResult.payload(), latitude, longitude, normalizedHours);
        }
        return WeatherFetchResult.success(forecastData);
    }

    @Override
    public Optional<WeatherData> fetchHydrologyCurrentConditions(HydrologyQuery query) {
        return nwpsHydrologyClient.fetchCurrentConditions(query);
    }

    @Override
    public Optional<WeatherData> fetchHydrologyForecastConditions(HydrologyQuery query) {
        return nwpsHydrologyClient.fetchForecastConditions(query);
    }

    @Override
    public WeatherFetchResult<Optional<WeatherData>> fetchHydrologyCurrentConditionsWithStatus(HydrologyQuery query) {
        return nwpsHydrologyClient.fetchCurrentConditionsWithStatus(query);
    }

    @Override
    public WeatherFetchResult<Optional<WeatherData>> fetchHydrologyForecastConditionsWithStatus(HydrologyQuery query) {
        return nwpsHydrologyClient.fetchForecastConditionsWithStatus(query);
    }

    private RequestResult<NoaaPointProperties> fetchPointProperties(double latitude, double longitude) {
        String normalizedLatitude = String.format(Locale.US, "%.4f", latitude);
        String normalizedLongitude = String.format(Locale.US, "%.4f", longitude);
        return requestWithFallback(
                "point_metadata",
                () -> noaaWebClient.get()
                        .uri("/points/{latitude},{longitude}", normalizedLatitude, normalizedLongitude)
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

    private RequestResult<NoaaForecastHourlyResponse> fetchHourlyForecast(String forecastHourlyUrl) {
        if (forecastHourlyUrl == null || forecastHourlyUrl.isBlank()) {
            return RequestResult.success(null);
        }
        return requestWithFallback(
                "hourly_forecast",
                () -> noaaWebClient.get()
                        .uri(forecastHourlyUrl)
                        .retrieve()
                        .bodyToMono(NoaaForecastHourlyResponse.class));
    }

    private RequestResult<NoaaGridpointForecastResponse> fetchGridForecast(String forecastGridDataUrl) {
        if (forecastGridDataUrl == null || forecastGridDataUrl.isBlank()) {
            return RequestResult.success(null);
        }
        return requestWithFallback(
                "forecast_grid_data",
                () -> noaaWebClient.get()
                        .uri(forecastGridDataUrl)
                        .retrieve()
                        .bodyToMono(NoaaGridpointForecastResponse.class));
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
        Double windDirection = props == null ? null : extractValue(props.getWindDirection());
        Double visibilityM = props == null ? null : extractValue(props.getVisibility());
        Double visibilityKm = visibilityM != null ? visibilityM / 1000.0 : null;
        Double dewPointC = extractCelsius(props == null ? null : props.getDewpoint());
        Double windChillC = extractCelsius(props == null ? null : props.getWindChill());
        Double heatIndexC = extractCelsius(props == null ? null : props.getHeatIndex());

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
                .windDirection(windDirection)
                .visibility(visibilityKm)
                .dewPoint(dewPointC)
                .windChill(windChillC)
                .heatIndex(heatIndexC)
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

    private void enrichForecastWithGridData(List<WeatherData> forecastData, NoaaGridpointForecastResponse gridResponse) {
        if (forecastData == null || forecastData.isEmpty() || gridResponse == null || gridResponse.getProperties() == null) {
            return;
        }
        NoaaGridpointProperties properties = gridResponse.getProperties();
        for (WeatherData weatherData : forecastData) {
            Instant onset = weatherData.getOnset();
            if (onset == null) {
                continue;
            }
            if (weatherData.getHumidity() == null) {
                weatherData.setHumidity(extractGridValue(properties.getRelativeHumidity(), onset));
            }
            weatherData.setDewPoint(extractGridCelsius(properties.getDewpoint(), onset));
            if (weatherData.getWindSpeed() == null) {
                weatherData.setWindSpeed(extractGridWindKmh(properties.getWindSpeed(), onset));
            }
            weatherData.setWindGust(extractGridWindKmh(properties.getWindGust(), onset));
            weatherData.setSkyCover(extractGridValue(properties.getSkyCover(), onset));
            if (weatherData.getPrecipitationProbability() == null) {
                weatherData.setPrecipitationProbability(extractGridValue(properties.getProbabilityOfPrecipitation(), onset));
            }
            weatherData.setPrecipitationAmount(extractGridMillimeters(properties.getQuantitativePrecipitation(), onset));
            if (weatherData.getPrecipitation() == null) {
                weatherData.setPrecipitation(weatherData.getPrecipitationProbability());
            }
            weatherData.setApparentTemperature(extractGridCelsius(properties.getApparentTemperature(), onset));
            weatherData.setWindChill(extractGridCelsius(properties.getWindChill(), onset));
            weatherData.setHeatIndex(extractGridCelsius(properties.getHeatIndex(), onset));
            weatherData.setVisibility(extractGridKilometers(properties.getVisibility(), onset));
            weatherData.setWindDirection(extractGridValue(properties.getWindDirection(), onset));
            weatherData.setSnowfallAmount(extractGridMillimeters(properties.getSnowfallAmount(), onset));
            weatherData.setIceAccumulation(extractGridMillimeters(properties.getIceAccumulation(), onset));
            weatherData.setProbabilityOfThunder(extractGridValue(properties.getProbabilityOfThunder(), onset));
            weatherData.setCeilingHeight(extractGridValue(properties.getCeilingHeight(), onset));
        }
    }

    private List<WeatherData> mapGridForecastToWeatherData(
            NoaaGridpointForecastResponse response,
            double latitude,
            double longitude,
            int forecastWindowHours) {
        if (response == null || response.getProperties() == null) {
            return List.of();
        }
        NoaaGridpointProperties properties = response.getProperties();
        TreeMap<Instant, Instant> slots = buildGridSlots(properties, forecastWindowHours);
        if (slots.isEmpty()) {
            return List.of();
        }

        List<WeatherData> results = new ArrayList<>();
        for (var slot : slots.entrySet()) {
            Instant onset = slot.getKey();
            WeatherData weatherData = WeatherData.builder()
                    .id("forecast-grid-" + latitude + "-" + longitude + "-" + onset.toEpochMilli())
                    .location(String.format(Locale.US, "lat=%.4f,lon=%.4f", latitude, longitude))
                    .latitude(latitude)
                    .longitude(longitude)
                    .eventType("FORECAST_CONDITIONS")
                    .headline("NOAA forecast grid conditions")
                    .description("Forecast grid data derived from NOAA forecastGridData")
                    .status("FORECAST")
                    .onset(onset)
                    .expires(slot.getValue())
                    .temperature(null)
                    .humidity(extractGridValue(properties.getRelativeHumidity(), onset))
                    .dewPoint(extractGridCelsius(properties.getDewpoint(), onset))
                    .windSpeed(extractGridWindKmh(properties.getWindSpeed(), onset))
                    .windGust(extractGridWindKmh(properties.getWindGust(), onset))
                    .skyCover(extractGridValue(properties.getSkyCover(), onset))
                    .precipitationProbability(extractGridValue(properties.getProbabilityOfPrecipitation(), onset))
                    .precipitationAmount(extractGridMillimeters(properties.getQuantitativePrecipitation(), onset))
                    .timestamp(Instant.now())
                    .build();
            weatherData.setPrecipitation(weatherData.getPrecipitationProbability());
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

    private Double extractGridValue(NoaaGridValueSeries series, Instant onset) {
        GridValueInterval interval = findGridInterval(series, onset);
        return interval == null ? null : interval.value();
    }

    private Double extractGridCelsius(NoaaGridValueSeries series, Instant onset) {
        GridValueInterval interval = findGridInterval(series, onset);
        if (interval == null) {
            return null;
        }
        return NoaaUnitConversionUtils.toCelsius(interval.value(), series.getUom());
    }

    private Double extractGridWindKmh(NoaaGridValueSeries series, Instant onset) {
        GridValueInterval interval = findGridInterval(series, onset);
        if (interval == null) {
            return null;
        }
        return NoaaUnitConversionUtils.toKilometersPerHour(interval.value(), series.getUom());
    }

    private Double extractGridMillimeters(NoaaGridValueSeries series, Instant onset) {
        GridValueInterval interval = findGridInterval(series, onset);
        if (interval == null) {
            return null;
        }
        return NoaaUnitConversionUtils.toMillimeters(interval.value(), series.getUom());
    }

    private Double extractGridKilometers(NoaaGridValueSeries series, Instant onset) {
        GridValueInterval interval = findGridInterval(series, onset);
        if (interval == null) {
            return null;
        }
        String uom = series.getUom();
        if (uom != null && uom.toLowerCase().contains(":m")) {
            return interval.value() / 1000.0;
        }
        return interval.value();
    }

    private GridValueInterval findGridInterval(NoaaGridValueSeries series, Instant onset) {
        if (series == null || series.getValues() == null || onset == null) {
            return null;
        }
        for (NoaaGridValueEntry value : series.getValues()) {
            GridValueInterval interval = parseGridInterval(value);
            if (interval == null) {
                continue;
            }
            if (!onset.isBefore(interval.start()) && onset.isBefore(interval.end())) {
                return interval;
            }
        }
        return null;
    }

    private TreeMap<Instant, Instant> buildGridSlots(NoaaGridpointProperties properties, int forecastWindowHours) {
        TreeMap<Instant, Instant> slots = new TreeMap<>();
        Instant cutoff = Instant.now().plus(Duration.ofHours(forecastWindowHours));
        collectGridSlots(slots, properties == null ? null : properties.getRelativeHumidity(), cutoff);
        collectGridSlots(slots, properties == null ? null : properties.getDewpoint(), cutoff);
        collectGridSlots(slots, properties == null ? null : properties.getWindSpeed(), cutoff);
        collectGridSlots(slots, properties == null ? null : properties.getWindGust(), cutoff);
        collectGridSlots(slots, properties == null ? null : properties.getSkyCover(), cutoff);
        collectGridSlots(slots, properties == null ? null : properties.getProbabilityOfPrecipitation(), cutoff);
        collectGridSlots(slots, properties == null ? null : properties.getQuantitativePrecipitation(), cutoff);
        return slots;
    }

    private void collectGridSlots(TreeMap<Instant, Instant> slots, NoaaGridValueSeries series, Instant cutoff) {
        if (series == null || series.getValues() == null) {
            return;
        }
        for (NoaaGridValueEntry value : series.getValues()) {
            GridValueInterval interval = parseGridInterval(value);
            if (interval == null || interval.start().isAfter(cutoff)) {
                continue;
            }
            slots.merge(interval.start(), interval.end(), (current, candidate) -> candidate.isAfter(current) ? candidate : current);
        }
    }

    private GridValueInterval parseGridInterval(NoaaGridValueEntry value) {
        if (value == null || value.getValidTime() == null || value.getValidTime().isBlank() || value.getValue() == null) {
            return null;
        }
        String[] parts = value.getValidTime().split("/", 2);
        if (parts.length != 2) {
            return null;
        }
        Instant start = parseInstantSafely(parts[0]);
        if (start == null) {
            return null;
        }
        try {
            Duration duration = Duration.parse(parts[1]);
            return new GridValueInterval(start, start.plus(duration), value.getValue());
        } catch (Exception ex) {
            log.warn("Unable to parse NOAA validTime duration: {}", value.getValidTime());
            return null;
        }
    }

    private String joinFailureReasons(String first, String second) {
        if (first == null || first.isBlank()) {
            return second;
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        return first + "; " + second;
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

    private record GridValueInterval(Instant start, Instant end, Double value) {
    }

    @Override
    public List<WeatherData> fetchObservationHistory(double latitude, double longitude, int hours) {
        int normalizedHours = Math.max(1, Math.min(hours, 24));
        log.info("Fetching observation history for: {}, {} with {}h window", latitude, longitude, normalizedHours);

        RequestResult<NoaaPointProperties> pointResult = fetchPointProperties(latitude, longitude);
        if (!pointResult.successful() || pointResult.payload() == null) return List.of();

        RequestResult<NoaaStationProperties> stationResult = fetchPrimaryStation(pointResult.payload().getObservationStations());
        if (!stationResult.successful() || stationResult.payload() == null) return List.of();

        String stationId = stationResult.payload().getStationIdentifier();
        String stationName = stationResult.payload().getName();
        Instant end = Instant.now();
        Instant start = end.minus(Duration.ofHours(normalizedHours));

        RequestResult<NoaaObservationListResponse> result = requestWithFallback(
            "observation_history",
            () -> noaaWebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/stations/{stationId}/observations")
                    .queryParam("start", start.toString())
                    .queryParam("end", end.toString())
                    .build(stationId))
                .retrieve()
                .bodyToMono(NoaaObservationListResponse.class));

        if (!result.successful() || result.payload() == null || result.payload().getFeatures() == null) {
            return List.of();
        }

        return result.payload().getFeatures().stream()
            .map(f -> mapObservationFeatureToWeatherData(f, stationId, stationName, latitude, longitude))
            .filter(Objects::nonNull)
            .toList();
    }

    private WeatherData mapObservationFeatureToWeatherData(
            NoaaObservationFeature feature,
            String stationId,
            String stationName,
            double latitude,
            double longitude) {
        if (feature == null) return null;
        NoaaObservationResponse wrapper = new NoaaObservationResponse();
        wrapper.setProperties(feature.getProperties());
        return mapObservationToWeatherData(wrapper, stationId, stationName, latitude, longitude);
    }

    @Override
    public List<WeatherData> fetchDailyForecast(double latitude, double longitude) {
        log.info("Fetching daily forecast for: {}, {}", latitude, longitude);

        RequestResult<NoaaPointProperties> pointResult = fetchPointProperties(latitude, longitude);
        if (!pointResult.successful() || pointResult.payload() == null) return List.of();

        String forecastUrl = pointResult.payload().getForecast();
        if (forecastUrl == null || forecastUrl.isBlank()) return List.of();

        RequestResult<NoaaDailyForecastResponse> result = requestWithFallback(
            "daily_forecast",
            () -> noaaWebClient.get()
                .uri(forecastUrl)
                .retrieve()
                .bodyToMono(NoaaDailyForecastResponse.class));

        if (!result.successful() || result.payload() == null || result.payload().getProperties() == null) {
            return List.of();
        }

        List<NoaaDailyForecastPeriod> periods = result.payload().getProperties().getPeriods();
        if (periods == null) return List.of();

        return periods.stream()
            .map(period -> mapDailyForecastPeriodToWeatherData(period, latitude, longitude))
            .toList();
    }

    private WeatherData mapDailyForecastPeriodToWeatherData(
            NoaaDailyForecastPeriod period,
            double latitude,
            double longitude) {
        Instant onset = parseInstantSafely(period.getStartTime());
        Instant expires = parseInstantSafely(period.getEndTime());
        Double tempC = NoaaUnitConversionUtils.toCelsius(
            period.getTemperature() == null ? null : period.getTemperature().doubleValue(),
            period.getTemperatureUnit());
        Double windKmh = NoaaUnitConversionUtils.parseWindSpeedToKmh(period.getWindSpeed());

        return WeatherData.builder()
            .id("daily-" + latitude + "-" + longitude + "-" + (onset != null ? onset.toEpochMilli() : UUID.randomUUID()))
            .location(String.format(Locale.US, "lat=%.4f,lon=%.4f", latitude, longitude))
            .latitude(latitude)
            .longitude(longitude)
            .eventType("DAILY_FORECAST")
            .headline(period.getName() != null ? period.getName() + ": " + period.getShortForecast() : period.getShortForecast())
            .description(period.getDetailedForecast() != null ? period.getDetailedForecast() : period.getShortForecast())
            .status("FORECAST")
            .onset(onset)
            .expires(expires)
            .temperature(tempC)
            .windSpeed(windKmh)
            .timestamp(Instant.now())
            .build();
    }

    @Override
    public Optional<WeatherData> fetchAlertById(String alertId) {
        log.info("Fetching alert by id: {}", alertId);

        RequestResult<NoaaSingleAlertResponse> result = requestWithFallback(
            "alert_by_id",
            () -> noaaWebClient.get()
                .uri("/alerts/{id}", alertId)
                .retrieve()
                .bodyToMono(NoaaSingleAlertResponse.class));

        if (!result.successful() || result.payload() == null) return Optional.empty();

        NoaaSingleAlertResponse response = result.payload();
        NoaaAlertProperties props = response.getProperties();
        if (props == null) return Optional.empty();

        WeatherData weatherData = WeatherData.builder()
            .id(response.getId() != null ? response.getId() : UUID.randomUUID().toString())
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

        return Optional.of(weatherData);
    }

    @Override
    public List<NwsProduct> fetchProductsByType(String typeCode, String locationCode) {
        log.info("Fetching NWS products: type={}, location={}", typeCode, locationCode);

        RequestResult<NoaaProductListResponse> result = requestWithFallback(
            "products_by_type",
            () -> noaaWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/products");
                    if (typeCode != null && !typeCode.isBlank()) builder = builder.queryParam("type", typeCode);
                    if (locationCode != null && !locationCode.isBlank()) builder = builder.queryParam("location", locationCode);
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(NoaaProductListResponse.class));

        if (!result.successful() || result.payload() == null || result.payload().getGraph() == null) {
            return List.of();
        }

        return result.payload().getGraph().stream()
            .map(this::mapProductItemToNwsProduct)
            .toList();
    }

    @Override
    public Optional<NwsProduct> fetchProductById(String productId) {
        log.info("Fetching NWS product by id: {}", productId);

        RequestResult<NoaaProductResponse> result = requestWithFallback(
            "product_by_id",
            () -> noaaWebClient.get()
                .uri("/products/{productId}", productId)
                .retrieve()
                .bodyToMono(NoaaProductResponse.class));

        if (!result.successful() || result.payload() == null) return Optional.empty();

        return Optional.of(mapProductResponseToNwsProduct(result.payload()));
    }

    private NwsProduct mapProductItemToNwsProduct(NoaaProductItem item) {
        return NwsProduct.builder()
            .id(item.getId())
            .wmoCollectiveId(item.getWmoCollectiveId())
            .issuingOffice(item.getIssuingOffice())
            .issuanceTime(parseInstantSafely(item.getIssuanceTime()))
            .productCode(item.getProductCode())
            .productName(item.getProductName())
            .build();
    }

    private NwsProduct mapProductResponseToNwsProduct(NoaaProductResponse response) {
        return NwsProduct.builder()
            .id(response.getId())
            .wmoCollectiveId(response.getWmoCollectiveId())
            .issuingOffice(response.getIssuingOffice())
            .issuanceTime(parseInstantSafely(response.getIssuanceTime()))
            .productCode(response.getProductCode())
            .productName(response.getProductName())
            .productText(response.getProductText())
            .build();
    }

    @Override
    public List<WeatherData> fetchZoneForecast(String zoneType, String zoneId) {
        log.info("Fetching zone forecast: type={}, id={}", zoneType, zoneId);

        RequestResult<NoaaZoneForecastResponse> result = requestWithFallback(
            "zone_forecast",
            () -> noaaWebClient.get()
                .uri("/zones/{type}/{zoneId}/forecast", zoneType, zoneId)
                .retrieve()
                .bodyToMono(NoaaZoneForecastResponse.class));

        if (!result.successful() || result.payload() == null || result.payload().getProperties() == null) {
            return List.of();
        }

        NoaaZoneForecastProperties props = result.payload().getProperties();
        if (props.getPeriods() == null) return List.of();

        Instant updated = parseInstantSafely(props.getUpdated());

        return props.getPeriods().stream()
            .map(period -> WeatherData.builder()
                .id("zone-" + zoneType + "-" + zoneId + "-" + period.getName().replaceAll("\\s+", "_"))
                .location(zoneId)
                .eventType("ZONE_FORECAST")
                .headline(period.getName())
                .description(period.getDetailedForecast())
                .status("FORECAST")
                .onset(updated)
                .timestamp(Instant.now())
                .build())
            .toList();
    }
}
