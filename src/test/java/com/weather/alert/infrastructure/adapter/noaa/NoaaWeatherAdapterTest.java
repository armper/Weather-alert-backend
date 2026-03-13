package com.weather.alert.infrastructure.adapter.noaa;

import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.model.WeatherPointMetadata;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NoaaWeatherAdapterTest {

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
    void shouldFetchCurrentConditionsAndNormalizeUnits() {
        String baseUrl = server.url("/").toString();
        String stationsUrl = server.url("/gridpoints/MLB/26,68/stations").toString();

        server.enqueue(jsonResponse("""
                {
                  "properties": {
                    "forecastHourly": "%sgridpoints/MLB/26,68/forecast/hourly",
                    "observationStations": "%s"
                  }
                }
                """.formatted(baseUrl, stationsUrl)));

        server.enqueue(jsonResponse("""
                {
                  "features": [
                    {
                      "properties": {
                        "stationIdentifier": "KORL",
                        "name": "Orlando Executive Airport"
                      }
                    }
                  ]
                }
                """));

        server.enqueue(jsonResponse("""
                {
                  "properties": {
                    "timestamp": "2026-02-24T19:10:00Z",
                    "textDescription": "Clear",
                    "temperature": {"unitCode":"wmoUnit:degF","value":68},
                    "windSpeed": {"unitCode":"wmoUnit:m_s-1","value":10},
                    "relativeHumidity": {"unitCode":"wmoUnit:percent","value":50},
                    "precipitationLastHour": {"unitCode":"wmoUnit:mm","value":2}
                  }
                }
                """));

        NoaaWeatherAdapter adapter = newAdapter(baseUrl, 2, 0, 100);

        Optional<WeatherData> current = adapter.fetchCurrentConditions(28.5383, -81.3792);

        assertTrue(current.isPresent());
        assertEquals("CURRENT_CONDITIONS", current.get().getEventType());
        assertEquals("Orlando Executive Airport", current.get().getLocation());
        assertEquals(20.0, current.get().getTemperature(), 0.01);
        assertEquals(36.0, current.get().getWindSpeed(), 0.01);
        assertEquals(2.0, current.get().getPrecipitationAmount(), 0.01);
        assertEquals(2.0, current.get().getPrecipitation(), 0.01);
        assertEquals(50.0, current.get().getHumidity(), 0.01);
    }

    @Test
    void shouldFetchForecastConditionsWithinWindowAndNormalize() {
        String forecastHourlyUrl = server.url("/gridpoints/MLB/26,68/forecast/hourly").toString();
        String forecastGridUrl = server.url("/gridpoints/MLB/26,68").toString();
        Instant now = Instant.now();
        Instant inOneHour = now.plus(1, ChronoUnit.HOURS);
        Instant inTwoHours = now.plus(2, ChronoUnit.HOURS);
        Instant inSeventyHours = now.plus(70, ChronoUnit.HOURS);
        Instant inSeventyOneHours = now.plus(71, ChronoUnit.HOURS);

        server.enqueue(jsonResponse("""
                {
                  "properties": {
                    "forecastHourly": "%s",
                    "forecastGridData": "%s",
                    "observationStations": "%s"
                  }
                }
                """.formatted(forecastHourlyUrl, forecastGridUrl, server.url("/gridpoints/MLB/26,68/stations"))));

        server.enqueue(jsonResponse("""
                {
                  "properties": {
                    "periods": [
                      {
                        "startTime": "%s",
                        "endTime": "%s",
                        "temperature": 59,
                        "temperatureUnit": "F",
                        "windSpeed": "5 to 10 mph",
                        "shortForecast": "Slight Chance Rain",
                        "detailedForecast": "Rain may start later.",
                        "probabilityOfPrecipitation": {"unitCode":"wmoUnit:percent","value":40},
                        "relativeHumidity": {"unitCode":"wmoUnit:percent","value":70}
                      },
                      {
                        "startTime": "%s",
                        "endTime": "%s",
                        "temperature": 80,
                        "temperatureUnit": "F",
                        "windSpeed": "15 mph",
                        "shortForecast": "Too Far Out",
                        "probabilityOfPrecipitation": {"unitCode":"wmoUnit:percent","value":10},
                        "relativeHumidity": {"unitCode":"wmoUnit:percent","value":40}
                      }
                    ]
                  }
                }
                """.formatted(inOneHour, inTwoHours, inSeventyHours, inSeventyOneHours)));

        server.enqueue(jsonResponse("""
                {
                  "properties": {
                    "relativeHumidity": {
                      "uom": "wmoUnit:percent",
                      "values": [
                        {"validTime":"%s/PT1H","value":72}
                      ]
                    },
                    "dewpoint": {
                      "uom": "wmoUnit:degC",
                      "values": [
                        {"validTime":"%s/PT1H","value":18}
                      ]
                    },
                    "windGust": {
                      "uom": "wmoUnit:km_h-1",
                      "values": [
                        {"validTime":"%s/PT1H","value":42}
                      ]
                    },
                    "skyCover": {
                      "uom": "wmoUnit:percent",
                      "values": [
                        {"validTime":"%s/PT1H","value":88}
                      ]
                    },
                    "quantitativePrecipitation": {
                      "uom": "wmoUnit:mm",
                      "values": [
                        {"validTime":"%s/PT1H","value":1.5}
                      ]
                    }
                  }
                }
                """.formatted(inOneHour, inOneHour, inOneHour, inOneHour, inOneHour)));

        NoaaWeatherAdapter adapter = newAdapter(server.url("/").toString(), 2, 0, 100);
        List<WeatherData> forecast = adapter.fetchForecastConditions(28.5383, -81.3792, 48);

        assertEquals(1, forecast.size());
        WeatherData period = forecast.get(0);
        assertEquals("FORECAST_CONDITIONS", period.getEventType());
        assertEquals(15.0, period.getTemperature(), 0.6);
        assertEquals(16.0, period.getWindSpeed(), 0.6);
        assertEquals(40.0, period.getPrecipitationProbability(), 0.01);
        assertEquals(40.0, period.getPrecipitation(), 0.01);
        assertEquals(70.0, period.getHumidity(), 0.01);
        assertEquals(18.0, period.getDewPoint(), 0.01);
        assertEquals(42.0, period.getWindGust(), 0.01);
        assertEquals(88.0, period.getSkyCover(), 0.01);
        assertEquals(1.5, period.getPrecipitationAmount(), 0.01);
        assertEquals(inOneHour.truncatedTo(ChronoUnit.SECONDS), period.getTimestamp().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(period.getOnset(), period.getTimestamp());
    }

    @Test
    void shouldUseForecastPeriodStartTimeForDailyForecastTimestamp() {
        String forecastUrl = server.url("/gridpoints/MLB/26,68/forecast").toString();
        Instant tomorrowMorning = Instant.parse("2026-03-13T12:00:00Z");
        Instant tomorrowEvening = Instant.parse("2026-03-13T23:00:00Z");

        server.enqueue(jsonResponse("""
                {
                  "properties": {
                    "forecast": "%s"
                  }
                }
                """.formatted(forecastUrl)));

        server.enqueue(jsonResponse("""
                {
                  "properties": {
                    "periods": [
                      {
                        "number": 1,
                        "name": "Friday",
                        "startTime": "%s",
                        "endTime": "%s",
                        "isDaytime": true,
                        "temperature": 77,
                        "temperatureUnit": "F",
                        "windSpeed": "10 mph",
                        "windDirection": "SE",
                        "shortForecast": "Sunny",
                        "detailedForecast": "Sunny through the day."
                      }
                    ]
                  }
                }
                """.formatted(tomorrowMorning, tomorrowEvening)));

        NoaaWeatherAdapter adapter = newAdapter(server.url("/").toString(), 2, 0, 100);

        List<WeatherData> forecast = adapter.fetchDailyForecast(28.5383, -81.3792);

        assertEquals(1, forecast.size());
        WeatherData period = forecast.get(0);
        assertEquals(tomorrowMorning, period.getOnset());
        assertEquals(tomorrowMorning, period.getTimestamp());
        assertEquals("DAILY_FORECAST", period.getEventType());
    }

    @Test
    void shouldFallbackToEmptyWhenNoaaReturnsError() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":\"boom\"}"));
        server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":\"boom\"}"));

        NoaaWeatherAdapter adapter = newAdapter(server.url("/").toString(), 2, 0, 100);

        Optional<WeatherData> current = adapter.fetchCurrentConditions(10.0, 10.0);
        List<WeatherData> forecast = adapter.fetchForecastConditions(10.0, 10.0, 48);

        assertTrue(current.isEmpty());
        assertTrue(forecast.isEmpty());
    }

    @Test
    void shouldFallbackToEmptyOnTimeout() {
        server.enqueue(new MockResponse()
                .setBody("{}")
                .setBodyDelay(2, TimeUnit.SECONDS));

        NoaaWeatherAdapter adapter = newAdapter(server.url("/").toString(), 1, 0, 100);
        List<WeatherData> forecast = adapter.fetchForecastConditions(10.0, 10.0, 48);

        assertTrue(forecast.isEmpty());
    }

    @Test
    void shouldFetchPointMetadataAndReuseCachedPointResponse() {
        server.enqueue(jsonResponse("""
                {
                  "properties": {
                    "county": "https://api.weather.gov/zones/county/KSC201",
                    "forecastZone": "https://api.weather.gov/zones/forecast/KSZ009",
                    "fireWeatherZone": "https://api.weather.gov/zones/fire/KSZ009",
                    "forecastOffice": "https://api.weather.gov/offices/TOP"
                  }
                }
                """));

        NoaaWeatherAdapter adapter = newAdapter(server.url("/").toString(), 2, 0, 100);

        Optional<WeatherPointMetadata> first = adapter.fetchPointMetadata(39.7456, -97.0892);
        Optional<WeatherPointMetadata> second = adapter.fetchPointMetadata(39.7456, -97.0892);

        assertTrue(first.isPresent());
        assertEquals("KSC201", first.get().countyZoneId());
        assertEquals(List.of("KSC201", "KSZ009"), first.get().zoneIds());
        assertEquals(first, second);
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void shouldShortCircuitRequestsWhenOutageGuardIsOpen() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":\"boom\"}"));

        WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
        NwpsHydrologyClient nwpsHydrologyClient = new NwpsHydrologyClient(
                webClient,
                new SimpleMeterRegistry(),
                2,
                0,
                100,
                0,
                1,
                30);
        NoaaWeatherAdapter adapter = new NoaaWeatherAdapter(
                webClient,
                nwpsHydrologyClient,
                new SimpleMeterRegistry(),
                2,
                0,
                100,
                0,
                60,
                180,
                1,
                30);

        Optional<WeatherData> first = adapter.fetchCurrentConditions(10.0, 10.0);
        Optional<WeatherData> second = adapter.fetchCurrentConditions(10.0, 10.0);

        assertTrue(first.isEmpty());
        assertTrue(second.isEmpty());
        assertEquals(1, server.getRequestCount());
    }

    private NoaaWeatherAdapter newAdapter(String baseUrl, long timeoutSeconds, long retries, long retryBackoffMillis) {
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        NwpsHydrologyClient nwpsHydrologyClient = new NwpsHydrologyClient(
                webClient,
                new SimpleMeterRegistry(),
                timeoutSeconds,
                retries,
                retryBackoffMillis,
                0,
                1000,
                30);
        return new NoaaWeatherAdapter(
                webClient,
                nwpsHydrologyClient,
                new SimpleMeterRegistry(),
                timeoutSeconds,
                retries,
                retryBackoffMillis,
                0,
                60,
                180,
                1000,
                30);
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
