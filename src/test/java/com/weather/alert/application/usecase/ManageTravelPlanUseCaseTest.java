package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.CreateTravelPlanRequest;
import com.weather.alert.application.exception.TravelPlanNotFoundException;
import com.weather.alert.domain.model.TravelPlan;
import com.weather.alert.domain.port.TravelPlanRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageTravelPlanUseCaseTest {

    @Mock
    private TravelPlanRepositoryPort travelPlanRepository;

    private ManageTravelPlanUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ManageTravelPlanUseCase(travelPlanRepository);
    }

    @Test
    void shouldCreatePlanWithDefaults() {
        // Given
        CreateTravelPlanRequest request = CreateTravelPlanRequest.builder()
                .name("NYC Trip")
                .destination("New York City")
                .startDate(LocalDate.of(2026, 6, 10))
                .endDate(LocalDate.of(2026, 6, 15))
                .build();

        when(travelPlanRepository.save(any(TravelPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TravelPlan created = useCase.createPlan("user-1", request);

        // Then
        assertNotNull(created.getId());
        assertEquals("user-1", created.getUserId());
        assertEquals("NYC Trip", created.getName());
        assertEquals("New York City", created.getDestination());
        assertEquals(LocalDate.of(2026, 6, 10), created.getStartDate());
        assertEquals(LocalDate.of(2026, 6, 15), created.getEndDate());
        assertTrue(created.getAlertsEnabled());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());
    }

    @Test
    void shouldCreatePlanWithAlertsDisabled() {
        // Given
        CreateTravelPlanRequest request = CreateTravelPlanRequest.builder()
                .name("Quiet Retreat")
                .destination("Montana")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 7))
                .alertsEnabled(false)
                .build();

        when(travelPlanRepository.save(any(TravelPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TravelPlan created = useCase.createPlan("user-1", request);

        // Then
        assertFalse(created.getAlertsEnabled());
    }

    @Test
    void shouldUpdatePlan() {
        // Given
        TravelPlan existing = TravelPlan.builder()
                .id("plan-1")
                .userId("user-1")
                .name("Old Name")
                .destination("Old City")
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 5))
                .alertsEnabled(true)
                .build();

        CreateTravelPlanRequest request = CreateTravelPlanRequest.builder()
                .name("Updated Trip")
                .destination("New City")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 10))
                .notes("Updated notes")
                .build();

        when(travelPlanRepository.findById("plan-1")).thenReturn(Optional.of(existing));
        when(travelPlanRepository.save(any(TravelPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TravelPlan updated = useCase.updatePlan("plan-1", request);

        // Then
        assertEquals("plan-1", updated.getId());
        assertEquals("user-1", updated.getUserId());
        assertEquals("Updated Trip", updated.getName());
        assertEquals("New City", updated.getDestination());
        assertEquals("Updated notes", updated.getNotes());
        // alertsEnabled should be preserved from existing when request has null
        assertTrue(updated.getAlertsEnabled());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentPlan() {
        // Given
        when(travelPlanRepository.findById("missing")).thenReturn(Optional.empty());
        CreateTravelPlanRequest request = CreateTravelPlanRequest.builder()
                .name("X")
                .destination("Y")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build();

        // When / Then
        assertThrows(TravelPlanNotFoundException.class, () -> useCase.updatePlan("missing", request));
    }

    @Test
    void shouldGetPlansForUser() {
        // Given
        TravelPlan plan1 = TravelPlan.builder().id("p1").userId("user-1").name("Trip A").build();
        TravelPlan plan2 = TravelPlan.builder().id("p2").userId("user-1").name("Trip B").build();
        when(travelPlanRepository.findByUserId("user-1")).thenReturn(List.of(plan1, plan2));

        // When
        List<TravelPlan> plans = useCase.getPlansForUser("user-1");

        // Then
        assertEquals(2, plans.size());
    }

    @Test
    void shouldDeletePlan() {
        // Given
        TravelPlan plan = TravelPlan.builder().id("plan-1").userId("user-1").name("Trip").build();
        when(travelPlanRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        // When
        useCase.deletePlan("plan-1");

        // Then
        verify(travelPlanRepository).delete("plan-1");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentPlan() {
        // Given
        when(travelPlanRepository.findById("missing")).thenReturn(Optional.empty());

        // When / Then
        assertThrows(TravelPlanNotFoundException.class, () -> useCase.deletePlan("missing"));
    }

    @Test
    void shouldGetPlanById() {
        // Given
        TravelPlan plan = TravelPlan.builder().id("plan-1").userId("user-1").name("Trip").build();
        when(travelPlanRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        // When
        TravelPlan found = useCase.getPlanById("plan-1");

        // Then
        assertEquals("plan-1", found.getId());
    }

    @Test
    void shouldThrowWhenPlanNotFoundById() {
        // Given
        when(travelPlanRepository.findById("missing")).thenReturn(Optional.empty());

        // When / Then
        assertThrows(TravelPlanNotFoundException.class, () -> useCase.getPlanById("missing"));
    }
}
