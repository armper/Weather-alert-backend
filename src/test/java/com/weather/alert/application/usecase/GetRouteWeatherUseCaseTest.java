package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.RouteWeatherResponse;
import com.weather.alert.application.exception.TravelPlanNotFoundException;
import com.weather.alert.domain.model.RouteWaypoint;
import com.weather.alert.domain.model.TravelPlan;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.TravelPlanRepositoryPort;
import com.weather.alert.domain.port.WeatherDataPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRouteWeatherUseCaseTest {

    @Mock
    private TravelPlanRepositoryPort travelPlanRepository;

    @Mock
    private WeatherDataPort weatherDataPort;

    private GetRouteWeatherUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetRouteWeatherUseCase(travelPlanRepository, weatherDataPort);
    }

    @Test
    void shouldReturnWeatherForEachWaypointOrderedBySequence() {
        RouteWaypoint wp1 = RouteWaypoint.builder().sequence(1).label("Start – Nashville, TN")
                .latitude(36.1627).longitude(-86.7816).build();
        RouteWaypoint wp2 = RouteWaypoint.builder().sequence(2).label("Stop – Memphis, TN")
                .latitude(35.1495).longitude(-90.0490).build();

        TravelPlan plan = TravelPlan.builder()
                .id("plan-1")
                .userId("driver-1")
                .name("Nashville to Memphis run")
                .destination("Memphis")
                .startDate(LocalDate.parse("2026-06-01"))
                .endDate(LocalDate.parse("2026-06-01"))
                .waypoints(List.of(wp2, wp1)) // intentionally out of order
                .alertsEnabled(true)
                .alertCoverageMode("ALL_ALERTS")
                .selectedAlertTopics(List.of())
                .linkedCriteriaIds(List.of())
                .build();

        WeatherData currentWp1 = WeatherData.builder()
                .id("obs-1").location("Nashville, TN").temperature(22.0).windSpeed(15.0).build();
        WeatherData alertWp2 = WeatherData.builder()
                .id("alert-1").location("Memphis, TN").eventType("Thunderstorm Warning").severity("SEVERE").build();

        when(travelPlanRepository.findById("plan-1")).thenReturn(Optional.of(plan));
        when(weatherDataPort.fetchCurrentConditions(36.1627, -86.7816)).thenReturn(Optional.of(currentWp1));
        when(weatherDataPort.fetchAlertsForLocation(36.1627, -86.7816)).thenReturn(List.of());
        when(weatherDataPort.fetchCurrentConditions(35.1495, -90.0490)).thenReturn(Optional.empty());
        when(weatherDataPort.fetchAlertsForLocation(35.1495, -90.0490)).thenReturn(List.of(alertWp2));

        List<RouteWeatherResponse> results = useCase.getRouteWeather("plan-1");

        assertEquals(2, results.size());
        // Results must be ordered by sequence regardless of input order
        assertEquals(1, results.get(0).getSequence());
        assertEquals("Start – Nashville, TN", results.get(0).getLabel());
        assertNotNull(results.get(0).getCurrentConditions());
        assertEquals(22.0, results.get(0).getCurrentConditions().getTemperature());
        assertTrue(results.get(0).getActiveAlerts().isEmpty());

        assertEquals(2, results.get(1).getSequence());
        assertEquals("Stop – Memphis, TN", results.get(1).getLabel());
        assertNull(results.get(1).getCurrentConditions());
        assertEquals(1, results.get(1).getActiveAlerts().size());
        assertEquals("Thunderstorm Warning", results.get(1).getActiveAlerts().get(0).getEventType());
    }

    @Test
    void shouldReturnEmptyListWhenPlanHasNoWaypoints() {
        TravelPlan plan = TravelPlan.builder()
                .id("plan-2")
                .userId("driver-1")
                .name("Single destination trip")
                .destination("Atlanta")
                .startDate(LocalDate.parse("2026-07-01"))
                .endDate(LocalDate.parse("2026-07-02"))
                .waypoints(List.of())
                .alertsEnabled(true)
                .alertCoverageMode("ALL_ALERTS")
                .selectedAlertTopics(List.of())
                .linkedCriteriaIds(List.of())
                .build();

        when(travelPlanRepository.findById("plan-2")).thenReturn(Optional.of(plan));

        List<RouteWeatherResponse> results = useCase.getRouteWeather("plan-2");

        assertTrue(results.isEmpty());
        verify(weatherDataPort, never()).fetchCurrentConditions(anyDouble(), anyDouble());
    }

    @Test
    void shouldThrowWhenTravelPlanNotFound() {
        when(travelPlanRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(TravelPlanNotFoundException.class, () -> useCase.getRouteWeather("missing"));
    }
}
