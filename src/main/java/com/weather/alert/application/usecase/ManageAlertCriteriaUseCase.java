package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
import com.weather.alert.application.exception.CriteriaNotFoundException;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Use case for managing alert criteria (Command)
 */
@Service
@RequiredArgsConstructor
public class ManageAlertCriteriaUseCase {
    
    private final AlertCriteriaRepositoryPort criteriaRepository;
    
    public AlertCriteria createCriteria(CreateAlertCriteriaRequest request) {
        AlertCriteria criteria = AlertCriteria.builder()
                .id(UUID.randomUUID().toString())
                .userId(request.getUserId())
                .location(request.getLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .radiusKm(request.getRadiusKm())
                .eventType(request.getEventType())
                .minSeverity(request.getMinSeverity())
                .maxTemperature(request.getMaxTemperature())
                .minTemperature(request.getMinTemperature())
                .maxWindSpeed(request.getMaxWindSpeed())
                .maxPrecipitation(request.getMaxPrecipitation())
                .temperatureThreshold(request.getTemperatureThreshold())
                .temperatureDirection(request.getTemperatureDirection())
                .rainThreshold(request.getRainThreshold())
                .rainThresholdType(request.getRainThresholdType())
                .monitorCurrent(defaultMonitorCurrent(request.getMonitorCurrent()))
                .monitorForecast(defaultMonitorForecast(request.getMonitorForecast()))
                .forecastWindowHours(defaultForecastWindowHours(request.getForecastWindowHours()))
                .temperatureUnit(defaultTemperatureUnit(request.getTemperatureUnit()))
                .oncePerEvent(defaultOncePerEvent(request.getOncePerEvent()))
                .rearmWindowMinutes(defaultRearmWindowMinutes(request.getRearmWindowMinutes()))
                .enabled(true)
                .build();
        
        return criteriaRepository.save(criteria);
    }
    
    public void deleteCriteria(String criteriaId) {
        if (criteriaRepository.findById(criteriaId).isEmpty()) {
            throw new CriteriaNotFoundException(criteriaId);
        }
        criteriaRepository.delete(criteriaId);
    }
    
    public AlertCriteria updateCriteria(String criteriaId, CreateAlertCriteriaRequest request) {
        return criteriaRepository.findById(criteriaId)
                .map(existing -> {
                    existing.setLocation(request.getLocation());
                    existing.setLatitude(request.getLatitude());
                    existing.setLongitude(request.getLongitude());
                    existing.setRadiusKm(request.getRadiusKm());
                    existing.setEventType(request.getEventType());
                    existing.setMinSeverity(request.getMinSeverity());
                    existing.setMaxTemperature(request.getMaxTemperature());
                    existing.setMinTemperature(request.getMinTemperature());
                    existing.setMaxWindSpeed(request.getMaxWindSpeed());
                    existing.setMaxPrecipitation(request.getMaxPrecipitation());
                    existing.setTemperatureThreshold(request.getTemperatureThreshold());
                    existing.setTemperatureDirection(request.getTemperatureDirection());
                    existing.setRainThreshold(request.getRainThreshold());
                    existing.setRainThresholdType(request.getRainThresholdType());
                    existing.setMonitorCurrent(defaultMonitorCurrent(request.getMonitorCurrent()));
                    existing.setMonitorForecast(defaultMonitorForecast(request.getMonitorForecast()));
                    existing.setForecastWindowHours(defaultForecastWindowHours(request.getForecastWindowHours()));
                    existing.setTemperatureUnit(defaultTemperatureUnit(request.getTemperatureUnit()));
                    existing.setOncePerEvent(defaultOncePerEvent(request.getOncePerEvent()));
                    existing.setRearmWindowMinutes(defaultRearmWindowMinutes(request.getRearmWindowMinutes()));
                    return criteriaRepository.save(existing);
                })
                .orElseThrow(() -> new CriteriaNotFoundException(criteriaId));
    }

    private boolean defaultMonitorCurrent(Boolean monitorCurrent) {
        return monitorCurrent == null || monitorCurrent;
    }

    private boolean defaultMonitorForecast(Boolean monitorForecast) {
        return monitorForecast == null || monitorForecast;
    }

    private int defaultForecastWindowHours(Integer forecastWindowHours) {
        return forecastWindowHours == null ? 48 : forecastWindowHours;
    }

    private AlertCriteria.TemperatureUnit defaultTemperatureUnit(AlertCriteria.TemperatureUnit temperatureUnit) {
        return temperatureUnit == null ? AlertCriteria.TemperatureUnit.F : temperatureUnit;
    }

    private boolean defaultOncePerEvent(Boolean oncePerEvent) {
        return oncePerEvent == null || oncePerEvent;
    }

    private int defaultRearmWindowMinutes(Integer rearmWindowMinutes) {
        return rearmWindowMinutes == null ? 0 : rearmWindowMinutes;
    }
}
