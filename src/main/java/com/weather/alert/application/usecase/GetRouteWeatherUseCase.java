package com.weather.alert.application.usecase;

import com.weather.alert.application.dto.RouteWeatherResponse;
import com.weather.alert.application.dto.WeatherDataResponse;
import com.weather.alert.domain.model.RouteWaypoint;
import com.weather.alert.domain.model.TravelPlan;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.TravelPlanRepositoryPort;
import com.weather.alert.domain.port.WeatherDataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Fetches current weather conditions and active NWS alerts for every waypoint
 * on a truck driver's route.
 */
@Service
@RequiredArgsConstructor
public class GetRouteWeatherUseCase {

    private final TravelPlanRepositoryPort travelPlanRepository;
    private final WeatherDataPort weatherDataPort;

    /**
     * Returns per-waypoint weather for the given travel plan.
     * If the plan has no waypoints an empty list is returned.
     *
     * @param travelPlanId the id of the travel plan
     * @return ordered list of route weather results, one entry per waypoint
     * @throws com.weather.alert.application.exception.TravelPlanNotFoundException when the plan does not exist
     */
    public List<RouteWeatherResponse> getRouteWeather(String travelPlanId) {
        TravelPlan plan = travelPlanRepository.findById(travelPlanId)
                .orElseThrow(() -> new com.weather.alert.application.exception.TravelPlanNotFoundException(travelPlanId));

        List<RouteWaypoint> waypoints = plan.getWaypoints();
        if (waypoints == null || waypoints.isEmpty()) {
            return List.of();
        }

        return waypoints.stream()
                .sorted(java.util.Comparator.comparingInt(RouteWaypoint::getSequence))
                .map(wp -> buildRouteWeather(wp))
                .collect(Collectors.toList());
    }

    private RouteWeatherResponse buildRouteWeather(RouteWaypoint waypoint) {
        Optional<WeatherData> current = weatherDataPort.fetchCurrentConditions(
                waypoint.getLatitude(), waypoint.getLongitude());

        List<WeatherData> alerts = weatherDataPort.fetchAlertsForLocation(
                waypoint.getLatitude(), waypoint.getLongitude());

        return RouteWeatherResponse.builder()
                .sequence(waypoint.getSequence())
                .label(waypoint.getLabel())
                .latitude(waypoint.getLatitude())
                .longitude(waypoint.getLongitude())
                .currentConditions(current.map(WeatherDataResponse::fromDomain).orElse(null))
                .activeAlerts(alerts.stream().map(WeatherDataResponse::fromDomain).collect(Collectors.toList()))
                .build();
    }
}
