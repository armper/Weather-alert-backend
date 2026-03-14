package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.TravelPlanRequest;
import com.weather.alert.application.exception.TravelPlanNotFoundException;
import com.weather.alert.domain.model.TravelPlan;
import com.weather.alert.domain.port.TravelPlanRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageTravelPlansUseCase {

    private final TravelPlanRepositoryPort travelPlanRepository;

    public List<TravelPlan> getByUserId(String userId) {
        return travelPlanRepository.findByUserId(userId);
    }

    public TravelPlan getById(String travelPlanId) {
        return travelPlanRepository.findById(travelPlanId)
                .orElseThrow(() -> new TravelPlanNotFoundException(travelPlanId));
    }

    public TravelPlan create(TravelPlanRequest request) {
        Instant now = Instant.now();
        return travelPlanRepository.save(TravelPlan.builder()
                .id(UUID.randomUUID().toString())
                .userId(request.getUserId())
                .name(normalizeText(request.getName()))
                .destination(normalizeText(request.getDestination()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .notes(normalizeOptionalText(request.getNotes()))
                .alertsEnabled(defaultAlertsEnabled(request.getAlertsEnabled()))
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    public TravelPlan update(String travelPlanId, TravelPlanRequest request) {
        TravelPlan existing = getById(travelPlanId);
        existing.setName(normalizeText(request.getName()));
        existing.setDestination(normalizeText(request.getDestination()));
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setLatitude(request.getLatitude());
        existing.setLongitude(request.getLongitude());
        existing.setNotes(normalizeOptionalText(request.getNotes()));
        existing.setAlertsEnabled(defaultAlertsEnabled(request.getAlertsEnabled()));
        existing.setUpdatedAt(Instant.now());
        return travelPlanRepository.save(existing);
    }

    public void delete(String travelPlanId) {
        getById(travelPlanId);
        travelPlanRepository.deleteById(travelPlanId);
    }

    private boolean defaultAlertsEnabled(Boolean alertsEnabled) {
        return alertsEnabled == null || alertsEnabled;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.replaceAll("\\s{2,}", " ");
    }
}
