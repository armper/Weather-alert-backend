package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.TravelPlanRequest;
import com.weather.alert.application.exception.BillingStateException;
import com.weather.alert.application.exception.TravelPlanNotFoundException;
import com.weather.alert.application.service.BillingPlanService;
import com.weather.alert.domain.model.BillingEntitlements;
import com.weather.alert.domain.model.TravelPlan;
import com.weather.alert.domain.model.User;
import com.weather.alert.domain.port.TravelPlanRepositoryPort;
import com.weather.alert.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManageTravelPlansUseCase {

    private static final String ALL_ALERTS = "ALL_ALERTS";
    private static final String TOPICS = "TOPICS";
    private static final String LINKED_RULES = "LINKED_RULES";
    private static final Set<String> ALLOWED_COVERAGE_MODES = Set.of(ALL_ALERTS, TOPICS, LINKED_RULES);
    private static final Set<String> ALLOWED_ALERT_TOPICS = Set.of("RAIN", "WIND", "HEAT", "COLD", "HUMIDITY", "SKY", "RIVER");

    private final TravelPlanRepositoryPort travelPlanRepository;
    private final UserRepositoryPort userRepository;
    private final BillingPlanService billingPlanService;

    public List<TravelPlan> getByUserId(String userId) {
        return travelPlanRepository.findByUserId(userId);
    }

    public TravelPlan getById(String travelPlanId) {
        return travelPlanRepository.findById(travelPlanId)
                .orElseThrow(() -> new TravelPlanNotFoundException(travelPlanId));
    }

    public TravelPlan create(TravelPlanRequest request) {
        enforceTravelPlanLimit(request.getUserId());
        Instant now = Instant.now();
        boolean alertsEnabled = defaultAlertsEnabled(request.getAlertsEnabled());
        String coverageMode = normalizeCoverageMode(request.getAlertCoverageMode(), alertsEnabled);
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
                .alertsEnabled(alertsEnabled)
                .alertCoverageMode(coverageMode)
                .selectedAlertTopics(normalizeAlertTopics(request.getSelectedAlertTopics(), coverageMode, alertsEnabled))
                .linkedCriteriaIds(normalizeCriteriaIds(request.getLinkedCriteriaIds(), coverageMode, alertsEnabled))
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    public TravelPlan update(String travelPlanId, TravelPlanRequest request) {
        TravelPlan existing = getById(travelPlanId);
        boolean alertsEnabled = defaultAlertsEnabled(request.getAlertsEnabled());
        String coverageMode = normalizeCoverageMode(request.getAlertCoverageMode(), alertsEnabled);
        existing.setName(normalizeText(request.getName()));
        existing.setDestination(normalizeText(request.getDestination()));
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setLatitude(request.getLatitude());
        existing.setLongitude(request.getLongitude());
        existing.setNotes(normalizeOptionalText(request.getNotes()));
        existing.setAlertsEnabled(alertsEnabled);
        existing.setAlertCoverageMode(coverageMode);
        existing.setSelectedAlertTopics(normalizeAlertTopics(request.getSelectedAlertTopics(), coverageMode, alertsEnabled));
        existing.setLinkedCriteriaIds(normalizeCriteriaIds(request.getLinkedCriteriaIds(), coverageMode, alertsEnabled));
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

    private void enforceTravelPlanLimit(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        BillingEntitlements entitlements = billingPlanService.resolveEntitlements(user);
        int maxTravelPlans = entitlements.getMaxTravelPlans();
        int existingTravelPlanCount = travelPlanRepository.findByUserId(userId).size();

        if (existingTravelPlanCount >= maxTravelPlans) {
            if (maxTravelPlans == 0) {
                throw new BillingStateException("Travel plans start on the plus plan. Upgrade to add trips.");
            }
            throw new BillingStateException("Your " + entitlements.getPlan().name().toLowerCase(Locale.ROOT)
                    + " plan allows up to " + maxTravelPlans
                    + " travel plan" + (maxTravelPlans == 1 ? "" : "s") + ".");
        }
    }

    private String normalizeCoverageMode(String alertCoverageMode, boolean alertsEnabled) {
        if (!alertsEnabled) {
            return ALL_ALERTS;
        }
        if (alertCoverageMode == null || alertCoverageMode.isBlank()) {
            return ALL_ALERTS;
        }
        String normalized = alertCoverageMode.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_COVERAGE_MODES.contains(normalized) ? normalized : ALL_ALERTS;
    }

    private List<String> normalizeAlertTopics(List<String> selectedAlertTopics, String coverageMode, boolean alertsEnabled) {
        if (!alertsEnabled || !TOPICS.equals(coverageMode) || selectedAlertTopics == null) {
            return List.of();
        }
        return selectedAlertTopics.stream()
                .filter(topic -> topic != null && !topic.isBlank())
                .map(topic -> topic.trim().toUpperCase(Locale.ROOT))
                .filter(ALLOWED_ALERT_TOPICS::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> normalizeCriteriaIds(List<String> linkedCriteriaIds, String coverageMode, boolean alertsEnabled) {
        if (!alertsEnabled || !LINKED_RULES.equals(coverageMode) || linkedCriteriaIds == null) {
            return List.of();
        }
        return linkedCriteriaIds.stream()
                .filter(criteriaId -> criteriaId != null && !criteriaId.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
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
