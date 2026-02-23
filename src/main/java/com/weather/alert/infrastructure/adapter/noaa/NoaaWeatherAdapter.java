package com.weather.alert.infrastructure.adapter.noaa;

import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.WeatherDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adapter for fetching weather data from NOAA API
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NoaaWeatherAdapter implements WeatherDataPort {
    
    private final WebClient noaaWebClient;
    
    @Override
    public List<WeatherData> fetchActiveAlerts() {
        try {
            log.info("Fetching active alerts from NOAA API");
            
            NoaaAlertResponse response = noaaWebClient
                    .get()
                    .uri("/alerts/active")
                    .retrieve()
                    .bodyToMono(NoaaAlertResponse.class)
                    .block();
            
            if (response != null && response.getFeatures() != null) {
                return mapToWeatherData(response.getFeatures());
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching active alerts from NOAA", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<WeatherData> fetchAlertsForLocation(double latitude, double longitude) {
        try {
            log.info("Fetching alerts for location: {}, {}", latitude, longitude);
            
            NoaaAlertResponse response = noaaWebClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/alerts/active")
                            .queryParam("point", latitude + "," + longitude)
                            .build())
                    .retrieve()
                    .bodyToMono(NoaaAlertResponse.class)
                    .block();
            
            if (response != null && response.getFeatures() != null) {
                return mapToWeatherData(response.getFeatures());
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching alerts for location", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<WeatherData> fetchAlertsForState(String stateCode) {
        try {
            log.info("Fetching alerts for state: {}", stateCode);
            
            NoaaAlertResponse response = noaaWebClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/alerts/active")
                            .queryParam("area", stateCode)
                            .build())
                    .retrieve()
                    .bodyToMono(NoaaAlertResponse.class)
                    .block();
            
            if (response != null && response.getFeatures() != null) {
                return mapToWeatherData(response.getFeatures());
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching alerts for state", e);
            return new ArrayList<>();
        }
    }
    
    private List<WeatherData> mapToWeatherData(List<NoaaAlertFeature> features) {
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
                .onset(props.getOnset() != null ? Instant.parse(props.getOnset()) : null)
                .expires(props.getExpires() != null ? Instant.parse(props.getExpires()) : null)
                .status(props.getStatus())
                .messageType(props.getMessageType())
                .category(props.getCategory())
                .urgency(props.getUrgency())
                .certainty(props.getCertainty())
                .timestamp(Instant.now())
                .build();
    }
}
