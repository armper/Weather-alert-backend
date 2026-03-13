package com.weather.alert.domain.model;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * NOAA point metadata used to match coordinate-based criteria against zone-based alerts.
 */
public record WeatherPointMetadata(
        String countyZoneId,
        String forecastZoneId,
        String fireWeatherZoneId,
        String forecastOfficeId,
        List<String> zoneIds) {

    public WeatherPointMetadata {
        zoneIds = zoneIds == null ? List.of() : List.copyOf(new LinkedHashSet<>(zoneIds));
    }
}
