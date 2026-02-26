package com.weather.alert.domain.service;

import com.weather.alert.domain.model.Alert;
import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.AlertCriteriaState;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.AlertCriteriaStateRepositoryPort;
import com.weather.alert.domain.port.AlertRepositoryPort;
import com.weather.alert.domain.port.NotificationPort;
import com.weather.alert.domain.port.WeatherDataPort;
import com.weather.alert.domain.port.WeatherFetchResult;
import com.weather.alert.domain.port.WeatherDataSearchPort;
import com.weather.alert.domain.service.evaluation.AlertCriteriaRuleEvaluator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertProcessingServiceTest {

    @Mock
    private WeatherDataPort weatherDataPort;

    @Mock
    private AlertCriteriaRepositoryPort criteriaRepository;

    @Mock
    private AlertRepositoryPort alertRepository;

    @Mock
    private NotificationPort notificationPort;

    @Mock
    private WeatherDataSearchPort searchPort;

    @Mock
    private AlertCriteriaStateRepositoryPort criteriaStateRepository;

    private AlertProcessingService service;

    @BeforeEach
    void setUp() {
        service = new AlertProcessingService(
                weatherDataPort,
                criteriaRepository,
                alertRepository,
                notificationPort,
                searchPort,
                criteriaStateRepository,
                new AlertCriteriaRuleEvaluator(),
                new SimpleMeterRegistry()
        );
        lenient().when(alertRepository.findByCriteriaIdAndEventKey(anyString(), anyString()))
                .thenReturn(Optional.empty());
    }

    @Test
    void shouldGenerateAlertFromCurrentConditionsWhenEnabled() {
        AlertCriteria criteria = AlertCriteria.builder()
                .id("criteria-1")
                .userId("dev-admin")
                .enabled(true)
                .latitude(28.5383)
                .longitude(-81.3792)
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .monitorCurrent(true)
                .monitorForecast(false)
                .build();

        WeatherData current = WeatherData.builder()
                .id("current-1")
                .location("Orlando Executive Airport")
                .latitude(28.5383)
                .longitude(-81.3792)
                .eventType("CURRENT_CONDITIONS")
                .temperature(13.0)
                .build();

        when(criteriaRepository.findAllEnabled()).thenReturn(List.of(criteria));
        when(weatherDataPort.fetchActiveAlertsWithStatus()).thenReturn(WeatherFetchResult.success(List.of()));
        when(weatherDataPort.fetchCurrentConditionsWithStatus(28.5383, -81.3792))
                .thenReturn(WeatherFetchResult.success(Optional.of(current)));
        when(criteriaStateRepository.findByCriteriaId(criteria.getId())).thenReturn(Optional.empty());
        when(criteriaStateRepository.save(any(AlertCriteriaState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processWeatherAlerts();

        verify(weatherDataPort, times(1)).fetchCurrentConditionsWithStatus(28.5383, -81.3792);
        verify(weatherDataPort, never()).fetchForecastConditionsWithStatus(anyDouble(), anyDouble(), anyInt());
        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(notificationPort, times(1)).publishAlert(any(Alert.class));
    }

    @Test
    void shouldGenerateSingleAlertFromFirstMatchingForecastPeriod() {
        AlertCriteria criteria = AlertCriteria.builder()
                .id("criteria-2")
                .userId("dev-admin")
                .enabled(true)
                .latitude(28.5383)
                .longitude(-81.3792)
                .eventType("Rain")
                .rainThreshold(40.0)
                .rainThresholdType(AlertCriteria.RainThresholdType.PROBABILITY)
                .monitorCurrent(false)
                .monitorForecast(true)
                .forecastWindowHours(48)
                .build();

        WeatherData forecastNonMatch = WeatherData.builder()
                .id("forecast-1")
                .eventType("FORECAST_CONDITIONS")
                .headline("Partly Cloudy")
                .precipitationProbability(10.0)
                .build();

        WeatherData forecastMatchOne = WeatherData.builder()
                .id("forecast-2")
                .eventType("FORECAST_CONDITIONS")
                .headline("Chance Rain Showers")
                .description("Rain developing later")
                .precipitationProbability(55.0)
                .build();

        WeatherData forecastMatchTwo = WeatherData.builder()
                .id("forecast-3")
                .eventType("FORECAST_CONDITIONS")
                .headline("Rain likely")
                .precipitationProbability(70.0)
                .build();

        when(criteriaRepository.findAllEnabled()).thenReturn(List.of(criteria));
        when(weatherDataPort.fetchActiveAlertsWithStatus()).thenReturn(WeatherFetchResult.success(List.of()));
        when(weatherDataPort.fetchForecastConditionsWithStatus(28.5383, -81.3792, 48))
                .thenReturn(WeatherFetchResult.success(List.of(forecastNonMatch, forecastMatchOne, forecastMatchTwo)));
        when(criteriaStateRepository.findByCriteriaId(criteria.getId())).thenReturn(Optional.empty());
        when(criteriaStateRepository.save(any(AlertCriteriaState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processWeatherAlerts();

        verify(weatherDataPort, never()).fetchCurrentConditionsWithStatus(anyDouble(), anyDouble());
        verify(weatherDataPort, times(1)).fetchForecastConditionsWithStatus(28.5383, -81.3792, 48);
        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(notificationPort, times(1)).publishAlert(any(Alert.class));
    }

    @Test
    void shouldSkipConditionCallsWhenCriteriaHasNoCoordinates() {
        AlertCriteria criteria = AlertCriteria.builder()
                .id("criteria-3")
                .userId("dev-admin")
                .enabled(true)
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .monitorCurrent(true)
                .monitorForecast(true)
                .build();

        when(criteriaRepository.findAllEnabled()).thenReturn(List.of(criteria));
        when(weatherDataPort.fetchActiveAlertsWithStatus()).thenReturn(WeatherFetchResult.success(List.of()));
        when(criteriaStateRepository.findByCriteriaId(criteria.getId())).thenReturn(Optional.empty());

        service.processWeatherAlerts();

        verify(weatherDataPort, never()).fetchCurrentConditionsWithStatus(anyDouble(), anyDouble());
        verify(weatherDataPort, never()).fetchForecastConditionsWithStatus(anyDouble(), anyDouble(), anyInt());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void shouldTriggerImmediateEvaluationForNewCriteria() {
        AlertCriteria criteria = AlertCriteria.builder()
                .id("criteria-4")
                .userId("dev-admin")
                .enabled(true)
                .latitude(28.5383)
                .longitude(-81.3792)
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .monitorCurrent(true)
                .monitorForecast(false)
                .build();

        WeatherData current = WeatherData.builder()
                .id("current-2")
                .eventType("CURRENT_CONDITIONS")
                .temperature(12.0)
                .build();

        when(weatherDataPort.fetchActiveAlertsWithStatus()).thenReturn(WeatherFetchResult.success(List.of()));
        when(weatherDataPort.fetchCurrentConditionsWithStatus(28.5383, -81.3792))
                .thenReturn(WeatherFetchResult.success(Optional.of(current)));
        when(criteriaStateRepository.findByCriteriaId(criteria.getId())).thenReturn(Optional.empty());
        when(criteriaStateRepository.save(any(AlertCriteriaState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Alert> generated = service.processCriteriaImmediately(criteria);

        assertEquals(1, generated.size());
        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(notificationPort, times(1)).publishAlert(any(Alert.class));
        verify(searchPort, atLeastOnce()).indexWeatherData(any(WeatherData.class));
    }

    @Test
    void shouldSkipSaveAndPublishWhenDuplicateEventKeyExists() {
        AlertCriteria criteria = AlertCriteria.builder()
                .id("criteria-dup")
                .userId("dev-admin")
                .enabled(true)
                .latitude(28.5383)
                .longitude(-81.3792)
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .monitorCurrent(true)
                .monitorForecast(false)
                .build();

        WeatherData current = WeatherData.builder()
                .id("current-dup")
                .eventType("CURRENT_CONDITIONS")
                .temperature(12.0)
                .build();

        Alert existing = Alert.builder()
                .id("existing-alert")
                .criteriaId("criteria-dup")
                .eventKey("current|criteria-dup|2026-01-01T10:00:00Z")
                .build();

        when(criteriaRepository.findAllEnabled()).thenReturn(List.of(criteria));
        when(weatherDataPort.fetchActiveAlertsWithStatus()).thenReturn(WeatherFetchResult.success(List.of()));
        when(weatherDataPort.fetchCurrentConditionsWithStatus(28.5383, -81.3792))
                .thenReturn(WeatherFetchResult.success(Optional.of(current)));
        when(criteriaStateRepository.findByCriteriaId(criteria.getId())).thenReturn(Optional.empty());
        when(criteriaStateRepository.save(any(AlertCriteriaState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findByCriteriaIdAndEventKey(eq("criteria-dup"), anyString())).thenReturn(Optional.of(existing));

        service.processWeatherAlerts();

        verify(alertRepository, never()).save(any(Alert.class));
        verify(notificationPort, never()).publishAlert(any(Alert.class));
    }

    @Test
    void shouldReuseCurrentConditionFetchForCriteriaSharingCoordinates() {
        AlertCriteria first = AlertCriteria.builder()
                .id("criteria-a")
                .userId("dev-admin")
                .enabled(true)
                .latitude(28.5383)
                .longitude(-81.3792)
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .monitorCurrent(true)
                .monitorForecast(false)
                .build();

        AlertCriteria second = AlertCriteria.builder()
                .id("criteria-b")
                .userId("dev-admin")
                .enabled(true)
                .latitude(28.5383)
                .longitude(-81.3792)
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .monitorCurrent(true)
                .monitorForecast(false)
                .build();

        WeatherData current = WeatherData.builder()
                .id("current-shared")
                .eventType("CURRENT_CONDITIONS")
                .temperature(12.0)
                .build();

        when(criteriaRepository.findAllEnabled()).thenReturn(List.of(first, second));
        when(weatherDataPort.fetchActiveAlertsWithStatus()).thenReturn(WeatherFetchResult.success(List.of()));
        when(weatherDataPort.fetchCurrentConditionsWithStatus(28.5383, -81.3792))
                .thenReturn(WeatherFetchResult.success(Optional.of(current)));
        when(criteriaStateRepository.findByCriteriaId(anyString())).thenReturn(Optional.empty());
        when(criteriaStateRepository.save(any(AlertCriteriaState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processWeatherAlerts();

        verify(weatherDataPort, times(1)).fetchCurrentConditionsWithStatus(28.5383, -81.3792);
        verify(alertRepository, times(2)).save(any(Alert.class));
    }

    @Test
    void shouldOnlyTriggerMatchingTemperatureCriteriaWhenDirectionsConflict() {
        AlertCriteria belowThreshold = AlertCriteria.builder()
                .id("criteria-below")
                .userId("dev-admin")
                .enabled(true)
                .latitude(28.5383)
                .longitude(-81.3792)
                .temperatureThreshold(69.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .monitorCurrent(true)
                .monitorForecast(false)
                .build();

        AlertCriteria aboveThreshold = AlertCriteria.builder()
                .id("criteria-above")
                .userId("dev-admin")
                .enabled(true)
                .latitude(28.5383)
                .longitude(-81.3792)
                .temperatureThreshold(93.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.ABOVE)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .monitorCurrent(true)
                .monitorForecast(false)
                .build();

        WeatherData current = WeatherData.builder()
                .id("current-18c")
                .eventType("CURRENT_CONDITIONS")
                .temperature(18.0) // 64.4F
                .build();

        when(criteriaRepository.findAllEnabled()).thenReturn(List.of(belowThreshold, aboveThreshold));
        when(weatherDataPort.fetchActiveAlertsWithStatus()).thenReturn(WeatherFetchResult.success(List.of()));
        when(weatherDataPort.fetchCurrentConditionsWithStatus(28.5383, -81.3792))
                .thenReturn(WeatherFetchResult.success(Optional.of(current)));
        when(criteriaStateRepository.findByCriteriaId(anyString())).thenReturn(Optional.empty());
        when(criteriaStateRepository.save(any(AlertCriteriaState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processWeatherAlerts();

        ArgumentCaptor<Alert> savedAlertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(savedAlertCaptor.capture());
        verify(notificationPort, times(1)).publishAlert(any(Alert.class));
        assertEquals("criteria-below", savedAlertCaptor.getValue().getCriteriaId());
        assertEquals("current-18c", savedAlertCaptor.getValue().getWeatherDataId());
    }

    @Test
    void shouldNotClearStateWhenProviderDataIsUnavailable() {
        AlertCriteria criteria = AlertCriteria.builder()
                .id("criteria-unavailable")
                .userId("dev-admin")
                .enabled(true)
                .latitude(28.5383)
                .longitude(-81.3792)
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .monitorCurrent(true)
                .monitorForecast(false)
                .build();

        when(criteriaRepository.findAllEnabled()).thenReturn(List.of(criteria));
        when(weatherDataPort.fetchActiveAlertsWithStatus()).thenReturn(WeatherFetchResult.success(List.of()));
        when(weatherDataPort.fetchCurrentConditionsWithStatus(28.5383, -81.3792))
                .thenReturn(WeatherFetchResult.failure(Optional.empty(), "upstream timeout"));

        service.processWeatherAlerts();

        verify(criteriaStateRepository, never()).save(any(AlertCriteriaState.class));
        verify(alertRepository, never()).save(any(Alert.class));
        verify(notificationPort, never()).publishAlert(any(Alert.class));
    }
}
