package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.PagedResult;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.WeatherDataSearchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherDataSearchRepositoryAdapter implements WeatherDataSearchPort {

    private final JpaWeatherDataRepository jpaRepository;

    @Override
    public void indexWeatherData(WeatherData weatherData) {
        if (weatherData == null || weatherData.getId() == null || weatherData.getId().isBlank()) {
            return;
        }
        try {
            jpaRepository.save(toEntity(weatherData));
        } catch (Exception ex) {
            log.error("Error indexing weather data {}", weatherData.getId(), ex);
        }
    }

    @Override
    public PagedResult<WeatherData> getActiveWeatherData(int page, int size) {
        Page<WeatherDataEntity> results = jpaRepository.findAllByOrderByRecordedAtDesc(PageRequest.of(page, size));
        return PagedResult.<WeatherData>builder()
                .items(results.getContent().stream().map(this::toDomain).toList())
                .page(results.getNumber())
                .size(results.getSize())
                .totalElements(results.getTotalElements())
                .totalPages(results.getTotalPages())
                .hasNext(results.hasNext())
                .hasPrevious(results.hasPrevious())
                .build();
    }

    @Override
    public List<WeatherData> searchByLocation(String location, int limit) {
        return jpaRepository.findByLocationContainingIgnoreCaseOrderByRecordedAtDesc(
                        location, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<WeatherData> searchByEventType(String eventType, int limit) {
        return jpaRepository.findByEventTypeContainingIgnoreCaseOrderByRecordedAtDesc(
                        eventType, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<WeatherData> searchBySeverity(String severity, int limit) {
        return jpaRepository.findBySeverityOrderByRecordedAtDesc(
                        severity, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long deleteWeatherDataOlderThan(Instant cutoff) {
        if (cutoff == null) {
            return 0L;
        }
        try {
            return jpaRepository.deleteByRecordedAtBefore(cutoff);
        } catch (Exception ex) {
            log.error("Error pruning weather data older than {}", cutoff, ex);
            return 0L;
        }
    }

    private WeatherDataEntity toEntity(WeatherData domain) {
        return WeatherDataEntity.builder()
                .id(domain.getId())
                .location(domain.getLocation())
                .latitude(domain.getLatitude())
                .longitude(domain.getLongitude())
                .eventType(domain.getEventType())
                .severity(domain.getSeverity())
                .headline(domain.getHeadline())
                .description(domain.getDescription())
                .onset(domain.getOnset())
                .expires(domain.getExpires())
                .status(domain.getStatus())
                .messageType(domain.getMessageType())
                .category(domain.getCategory())
                .urgency(domain.getUrgency())
                .certainty(domain.getCertainty())
                .temperature(domain.getTemperature())
                .windSpeed(domain.getWindSpeed())
                .precipitation(domain.getPrecipitation())
                .precipitationProbability(domain.getPrecipitationProbability())
                .precipitationAmount(domain.getPrecipitationAmount())
                .humidity(domain.getHumidity())
                .dewPoint(domain.getDewPoint())
                .windGust(domain.getWindGust())
                .skyCover(domain.getSkyCover())
                .riverGaugeId(domain.getRiverGaugeId())
                .riverObservedStage(domain.getRiverObservedStage())
                .riverForecastStage(domain.getRiverForecastStage())
                .riverFloodStage(domain.getRiverFloodStage())
                .riverActionStage(domain.getRiverActionStage())
                .riverObservedCategory(domain.getRiverObservedCategory())
                .riverForecastCategory(domain.getRiverForecastCategory())
                .riverStageUnit(domain.getRiverStageUnit())
                .riverDistanceKm(domain.getRiverDistanceKm())
                .recordedAt(domain.getTimestamp())
                .apparentTemperature(domain.getApparentTemperature())
                .windChill(domain.getWindChill())
                .heatIndex(domain.getHeatIndex())
                .visibility(domain.getVisibility())
                .windDirection(domain.getWindDirection())
                .snowfallAmount(domain.getSnowfallAmount())
                .iceAccumulation(domain.getIceAccumulation())
                .probabilityOfThunder(domain.getProbabilityOfThunder())
                .ceilingHeight(domain.getCeilingHeight())
                .build();
    }

    private WeatherData toDomain(WeatherDataEntity entity) {
        return WeatherData.builder()
                .id(entity.getId())
                .location(entity.getLocation())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .eventType(entity.getEventType())
                .severity(entity.getSeverity())
                .headline(entity.getHeadline())
                .description(entity.getDescription())
                .onset(entity.getOnset())
                .expires(entity.getExpires())
                .status(entity.getStatus())
                .messageType(entity.getMessageType())
                .category(entity.getCategory())
                .urgency(entity.getUrgency())
                .certainty(entity.getCertainty())
                .temperature(entity.getTemperature())
                .windSpeed(entity.getWindSpeed())
                .precipitation(entity.getPrecipitation())
                .precipitationProbability(entity.getPrecipitationProbability())
                .precipitationAmount(entity.getPrecipitationAmount())
                .humidity(entity.getHumidity())
                .dewPoint(entity.getDewPoint())
                .windGust(entity.getWindGust())
                .skyCover(entity.getSkyCover())
                .riverGaugeId(entity.getRiverGaugeId())
                .riverObservedStage(entity.getRiverObservedStage())
                .riverForecastStage(entity.getRiverForecastStage())
                .riverFloodStage(entity.getRiverFloodStage())
                .riverActionStage(entity.getRiverActionStage())
                .riverObservedCategory(entity.getRiverObservedCategory())
                .riverForecastCategory(entity.getRiverForecastCategory())
                .riverStageUnit(entity.getRiverStageUnit())
                .riverDistanceKm(entity.getRiverDistanceKm())
                .timestamp(entity.getRecordedAt())
                .apparentTemperature(entity.getApparentTemperature())
                .windChill(entity.getWindChill())
                .heatIndex(entity.getHeatIndex())
                .visibility(entity.getVisibility())
                .windDirection(entity.getWindDirection())
                .snowfallAmount(entity.getSnowfallAmount())
                .iceAccumulation(entity.getIceAccumulation())
                .probabilityOfThunder(entity.getProbabilityOfThunder())
                .ceilingHeight(entity.getCeilingHeight())
                .build();
    }
}
