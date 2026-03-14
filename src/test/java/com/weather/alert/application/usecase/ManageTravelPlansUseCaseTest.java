package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.TravelPlanRequest;
import com.weather.alert.application.exception.TravelPlanNotFoundException;
import com.weather.alert.domain.model.TravelPlan;
import com.weather.alert.domain.port.TravelPlanRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageTravelPlansUseCaseTest {

    @Mock
    private TravelPlanRepositoryPort travelPlanRepository;

    private ManageTravelPlansUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ManageTravelPlansUseCase(travelPlanRepository);
    }

    @Test
    void shouldCreateTravelPlanWithDefaultAlertsEnabled() {
        TravelPlanRequest request = TravelPlanRequest.builder()
                .userId("user-1")
                .name("  NYC Conference  ")
                .destination("  New York City  ")
                .startDate(LocalDate.parse("2026-05-18"))
                .endDate(LocalDate.parse("2026-05-22"))
                .notes("  Bring layers for the evenings.  ")
                .build();

        when(travelPlanRepository.save(any(TravelPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TravelPlan saved = useCase.create(request);

        ArgumentCaptor<TravelPlan> captor = ArgumentCaptor.forClass(TravelPlan.class);
        verify(travelPlanRepository).save(captor.capture());
        assertNotNull(saved.getId());
        assertEquals("user-1", saved.getUserId());
        assertEquals("NYC Conference", captor.getValue().getName());
        assertEquals("New York City", captor.getValue().getDestination());
        assertEquals("Bring layers for the evenings.", captor.getValue().getNotes());
        assertEquals(Boolean.TRUE, captor.getValue().getAlertsEnabled());
        assertEquals("ALL_ALERTS", captor.getValue().getAlertCoverageMode());
        assertIterableEquals(List.of(), captor.getValue().getSelectedAlertTopics());
        assertIterableEquals(List.of(), captor.getValue().getLinkedCriteriaIds());
        assertNotNull(captor.getValue().getCreatedAt());
        assertNotNull(captor.getValue().getUpdatedAt());
    }

    @Test
    void shouldNormalizeTopicCoverageWhenCreatingTravelPlan() {
        TravelPlanRequest request = TravelPlanRequest.builder()
                .userId("user-1")
                .name("Beach weekend")
                .destination("Tampa")
                .startDate(LocalDate.parse("2026-07-01"))
                .endDate(LocalDate.parse("2026-07-03"))
                .alertsEnabled(true)
                .alertCoverageMode("topics")
                .selectedAlertTopics(List.of("rain", "RAIN", "wind", "unknown"))
                .linkedCriteriaIds(List.of("criteria-1"))
                .build();

        when(travelPlanRepository.save(any(TravelPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TravelPlan saved = useCase.create(request);

        assertEquals("TOPICS", saved.getAlertCoverageMode());
        assertIterableEquals(List.of("RAIN", "WIND"), saved.getSelectedAlertTopics());
        assertIterableEquals(List.of(), saved.getLinkedCriteriaIds());
    }

    @Test
    void shouldUpdateExistingTravelPlan() {
        TravelPlan existing = TravelPlan.builder()
                .id("trip-1")
                .userId("user-1")
                .name("Chicago")
                .destination("Chicago")
                .startDate(LocalDate.parse("2026-06-01"))
                .endDate(LocalDate.parse("2026-06-03"))
                .alertsEnabled(true)
                .alertCoverageMode("ALL_ALERTS")
                .selectedAlertTopics(List.of())
                .linkedCriteriaIds(List.of())
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        TravelPlanRequest request = TravelPlanRequest.builder()
                .userId("user-1")
                .name("Chicago client visit")
                .destination("Chicago, IL")
                .startDate(LocalDate.parse("2026-06-02"))
                .endDate(LocalDate.parse("2026-06-04"))
                .notes("Window seat if possible")
                .alertsEnabled(true)
                .alertCoverageMode("LINKED_RULES")
                .selectedAlertTopics(List.of("RAIN"))
                .linkedCriteriaIds(List.of("rule-1", " rule-1 ", "rule-2"))
                .build();

        when(travelPlanRepository.findById("trip-1")).thenReturn(Optional.of(existing));
        when(travelPlanRepository.save(any(TravelPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TravelPlan updated = useCase.update("trip-1", request);

        assertEquals("Chicago client visit", updated.getName());
        assertEquals("Chicago, IL", updated.getDestination());
        assertEquals(LocalDate.parse("2026-06-02"), updated.getStartDate());
        assertEquals(LocalDate.parse("2026-06-04"), updated.getEndDate());
        assertEquals("Window seat if possible", updated.getNotes());
        assertEquals("LINKED_RULES", updated.getAlertCoverageMode());
        assertIterableEquals(List.of(), updated.getSelectedAlertTopics());
        assertIterableEquals(List.of("rule-1", "rule-2"), updated.getLinkedCriteriaIds());
        assertEquals(Boolean.TRUE, updated.getAlertsEnabled());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), updated.getCreatedAt());
    }

    @Test
    void shouldReturnPlansOrderedFromRepository() {
        List<TravelPlan> plans = List.of(
                TravelPlan.builder().id("trip-1").userId("user-1").build(),
                TravelPlan.builder().id("trip-2").userId("user-1").build());
        when(travelPlanRepository.findByUserId("user-1")).thenReturn(plans);

        List<TravelPlan> result = useCase.getByUserId("user-1");

        assertEquals(2, result.size());
        assertEquals("trip-1", result.get(0).getId());
    }

    @Test
    void shouldDeleteExistingTravelPlan() {
        when(travelPlanRepository.findById("trip-1")).thenReturn(Optional.of(TravelPlan.builder()
                .id("trip-1")
                .userId("user-1")
                .build()));

        useCase.delete("trip-1");

        verify(travelPlanRepository).deleteById("trip-1");
    }

    @Test
    void shouldThrowWhenTravelPlanMissing() {
        when(travelPlanRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(TravelPlanNotFoundException.class, () -> useCase.getById("missing"));
    }
}
