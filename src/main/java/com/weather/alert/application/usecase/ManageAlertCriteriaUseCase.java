package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.CreateAlertCriteriaRequest;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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
                .enabled(true)
                .build();
        
        return criteriaRepository.save(criteria);
    }
    
    public void deleteCriteria(String criteriaId) {
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
                    return criteriaRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Criteria not found"));
    }
}
