package com.weather.alert.infrastructure.adapter.persistence;

import com.weather.alert.domain.model.PagedResult;
import com.weather.alert.domain.model.WeatherData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(WeatherDataSearchRepositoryAdapter.class)
class WeatherDataSearchRepositoryAdapterTest {

    @Autowired
    private WeatherDataSearchRepositoryAdapter adapter;

    @Test
    void shouldIndexAndPageWeatherDataByNewestFirst() {
        adapter.indexWeatherData(sample("older-1", "Seattle", "Flood Warning", "Moderate", Instant.parse("2026-03-07T10:00:00Z")));
        adapter.indexWeatherData(sample("newer-1", "Seattle", "High Wind Warning", "Severe", Instant.parse("2026-03-07T11:00:00Z")));

        PagedResult<WeatherData> result = adapter.getActiveWeatherData(0, 10);

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems()).extracting(WeatherData::getId)
                .containsExactly("newer-1", "older-1");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void shouldSearchLocationAndEventTypeIgnoringCase() {
        adapter.indexWeatherData(sample("weather-1", "Orlando", "Flood Warning", "Moderate", Instant.parse("2026-03-07T11:00:00Z")));
        adapter.indexWeatherData(sample("weather-2", "Albany", "Heat Advisory", "Minor", Instant.parse("2026-03-07T12:00:00Z")));

        List<WeatherData> locationResults = adapter.searchByLocation("orlan");
        List<WeatherData> eventResults = adapter.searchByEventType("flood");

        assertThat(locationResults).extracting(WeatherData::getId).containsExactly("weather-1");
        assertThat(eventResults).extracting(WeatherData::getId).containsExactly("weather-1");
    }

    @Test
    void shouldDeleteOlderWeatherData() {
        adapter.indexWeatherData(sample("old-1", "Miami", "Flood Warning", "Moderate", Instant.parse("2026-03-07T10:00:00Z")));
        adapter.indexWeatherData(sample("new-1", "Miami", "Flood Warning", "Moderate", Instant.parse("2026-03-07T12:00:00Z")));

        long deleted = adapter.deleteWeatherDataOlderThan(Instant.parse("2026-03-07T11:00:00Z"));

        assertThat(deleted).isEqualTo(1);
        assertThat(adapter.getActiveWeatherData(0, 10).getItems())
                .extracting(WeatherData::getId)
                .containsExactly("new-1");
    }

    @Test
    void shouldPersistLocationLongerThanLegacyVarcharLimit() {
        String longLocation = "Orlando; " + "Seminole, Orange, Osceola, Lake, Volusia, Brevard, Polk, "
                .repeat(8);

        adapter.indexWeatherData(sample("long-location-1", longLocation, "Flood Warning", "Moderate",
                Instant.parse("2026-03-07T12:00:00Z")));

        PagedResult<WeatherData> result = adapter.getActiveWeatherData(0, 10);

        assertThat(result.getItems()).extracting(WeatherData::getId).contains("long-location-1");
        assertThat(result.getItems().stream()
                .filter(item -> "long-location-1".equals(item.getId()))
                .findFirst()
                .map(WeatherData::getLocation))
                .contains(longLocation);
    }

    private WeatherData sample(String id, String location, String eventType, String severity, Instant timestamp) {
        return WeatherData.builder()
                .id(id)
                .location(location)
                .eventType(eventType)
                .severity(severity)
                .headline(eventType + " for " + location)
                .description("Test weather data")
                .temperature(24.0)
                .humidity(70.0)
                .timestamp(timestamp)
                .build();
    }
}
