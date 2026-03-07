package com.weather.alert.infrastructure.adapter.noaa;

import com.weather.alert.domain.model.HydrologyQuery;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.WeatherFetchResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NwpsHydrologyClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldResolveNearestGaugeAndReturnObservedRiverConditions() {
        server.enqueue(jsonResponse("""
                {
                  "gauges": [
                    {
                      "lid": "ABNG1",
                      "name": "Flint River (GA) at Albany",
                      "latitude": 31.5941,
                      "longitude": -84.1441
                    }
                  ]
                }
                """));
        server.enqueue(jsonResponse("""
                {
                  "lid": "ABNG1",
                  "name": "Flint River (GA) at Albany",
                  "state": {"abbreviation": "GA"},
                  "latitude": 31.5941,
                  "longitude": -84.1441,
                  "status": {
                    "observed": {
                      "primary": 26.8,
                      "primaryUnit": "ft",
                      "floodCategory": "minor",
                      "validTime": "2026-03-06T21:15:00Z"
                    },
                    "forecast": {
                      "primary": 31.4,
                      "primaryUnit": "ft",
                      "floodCategory": "moderate",
                      "validTime": "2026-03-07T00:00:00Z"
                    }
                  },
                  "flood": {
                    "stageUnits": "ft",
                    "categories": {
                      "action": {"stage": 16.0},
                      "minor": {"stage": 26.0}
                    }
                  }
                }
                """));

        NwpsHydrologyClient client = newClient(server.url("/").toString());
        Optional<WeatherData> current = client.fetchCurrentConditions(HydrologyQuery.builder()
                .latitude(31.59)
                .longitude(-84.14)
                .searchRadiusKm(30.0)
                .build());

        assertTrue(current.isPresent());
        assertEquals("RIVER_CURRENT_CONDITIONS", current.get().getEventType());
        assertEquals("ABNG1", current.get().getRiverGaugeId());
        assertEquals(26.8, current.get().getRiverObservedStage());
        assertEquals(31.4, current.get().getRiverForecastStage());
        assertEquals(26.0, current.get().getRiverFloodStage());
        assertEquals("minor", current.get().getRiverObservedCategory());
    }

    @Test
    void shouldReturnEmptyForecastWhenGaugeHasNoCurrentForecast() {
        server.enqueue(jsonResponse("""
                {
                  "lid": "TREF1",
                  "name": "Santa Fe River at Three Rivers Estates",
                  "state": {"abbreviation": "FL"},
                  "status": {
                    "observed": {
                      "primary": 7.07,
                      "primaryUnit": "ft",
                      "floodCategory": "low_threshold",
                      "validTime": "2026-03-06T20:00:00Z"
                    },
                    "forecast": {
                      "primary": -999,
                      "primaryUnit": "",
                      "floodCategory": "fcst_not_current",
                      "validTime": "0001-01-01T00:00:00Z"
                    }
                  },
                  "flood": {
                    "stageUnits": "ft",
                    "categories": {
                      "action": {"stage": 14.0},
                      "minor": {"stage": 17.0}
                    }
                  }
                }
                """));

        NwpsHydrologyClient client = newClient(server.url("/").toString());
        WeatherFetchResult<Optional<WeatherData>> forecast = client.fetchForecastConditionsWithStatus(HydrologyQuery.builder()
                .gaugeId("TREF1")
                .build());

        assertTrue(forecast.successful());
        assertTrue(forecast.data().isEmpty());
    }

    private NwpsHydrologyClient newClient(String baseUrl) {
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        return new NwpsHydrologyClient(
                webClient,
                new SimpleMeterRegistry(),
                2,
                0,
                100,
                0,
                1000,
                30);
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
