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
import com.weather.alert.domain.port.WeatherDataSearchPort;
import com.weather.alert.domain.service.evaluation.AlertCriteriaRuleEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertProcessingAntiSpamIntegrationTest {

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

    private InMemoryCriteriaStateRepository criteriaStateRepository;
    private AlertProcessingService service;

    @BeforeEach
    void setUp() {
        criteriaStateRepository = new InMemoryCriteriaStateRepository();
        service = new AlertProcessingService(
                weatherDataPort,
                criteriaRepository,
                alertRepository,
                notificationPort,
                searchPort,
                criteriaStateRepository,
                new AlertCriteriaRuleEvaluator()
        );
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findByCriteriaIdAndEventKey(anyString(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    void shouldNotifyOnlyOnceWhileConditionStaysMet() {
        AlertCriteria criteria = defaultTemperatureCriteria("criteria-spam-1");
        WeatherData met = currentAtTemp(12.0);

        when(criteriaRepository.findAllEnabled()).thenReturn(List.of(criteria));
        when(weatherDataPort.fetchActiveAlerts()).thenReturn(List.of());
        when(weatherDataPort.fetchCurrentConditions(28.5383, -81.3792))
                .thenReturn(Optional.of(met), Optional.of(met));

        service.processWeatherAlerts();
        service.processWeatherAlerts();

        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(notificationPort, times(1)).publishAlert(any(Alert.class));
    }

    @Test
    void shouldRearmAfterConditionClearsAndTriggerAgain() {
        AlertCriteria criteria = defaultTemperatureCriteria("criteria-spam-2");
        WeatherData met = currentAtTemp(12.0);
        WeatherData notMet = currentAtTemp(18.0);
        WeatherData metAgain = currentAtTemp(11.5);

        when(criteriaRepository.findAllEnabled()).thenReturn(List.of(criteria));
        when(weatherDataPort.fetchActiveAlerts()).thenReturn(List.of());
        when(weatherDataPort.fetchCurrentConditions(28.5383, -81.3792))
                .thenReturn(Optional.of(met), Optional.of(notMet), Optional.of(metAgain));

        service.processWeatherAlerts();
        service.processWeatherAlerts();
        service.processWeatherAlerts();

        verify(alertRepository, times(2)).save(any(Alert.class));
        verify(notificationPort, times(2)).publishAlert(any(Alert.class));
    }

    @Test
    void shouldSuppressReoccurrenceInsideRearmWindow() {
        AlertCriteria criteria = defaultTemperatureCriteria("criteria-spam-3", 120);
        WeatherData met = currentAtTemp(12.0);
        WeatherData notMet = currentAtTemp(18.0);
        WeatherData metAgain = currentAtTemp(11.0);

        when(criteriaRepository.findAllEnabled()).thenReturn(List.of(criteria));
        when(weatherDataPort.fetchActiveAlerts()).thenReturn(List.of());
        when(weatherDataPort.fetchCurrentConditions(28.5383, -81.3792))
                .thenReturn(Optional.of(met), Optional.of(notMet), Optional.of(metAgain));

        service.processWeatherAlerts();
        service.processWeatherAlerts();
        service.processWeatherAlerts();

        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(notificationPort, times(1)).publishAlert(any(Alert.class));
    }

    @Test
    void shouldAllowNewConditionWhenRearmWindowElapsed() {
        AlertCriteria criteria = defaultTemperatureCriteria("criteria-spam-4", 30);
        criteriaStateRepository.save(AlertCriteriaState.builder()
                .criteriaId(criteria.getId())
                .lastConditionMet(false)
                .lastEventSignature("current|" + criteria.getId())
                .lastNotifiedAt(Instant.now().minusSeconds(3600))
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build());

        when(criteriaRepository.findAllEnabled()).thenReturn(List.of(criteria));
        when(weatherDataPort.fetchActiveAlerts()).thenReturn(List.of());
        when(weatherDataPort.fetchCurrentConditions(28.5383, -81.3792)).thenReturn(Optional.of(currentAtTemp(12.0)));

        service.processWeatherAlerts();

        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(notificationPort, times(1)).publishAlert(any(Alert.class));
    }

    private AlertCriteria defaultTemperatureCriteria(String id) {
        return defaultTemperatureCriteria(id, 0);
    }

    private AlertCriteria defaultTemperatureCriteria(String id, int rearmWindowMinutes) {
        return AlertCriteria.builder()
                .id(id)
                .userId("dev-admin")
                .enabled(true)
                .latitude(28.5383)
                .longitude(-81.3792)
                .temperatureThreshold(60.0)
                .temperatureDirection(AlertCriteria.TemperatureDirection.BELOW)
                .temperatureUnit(AlertCriteria.TemperatureUnit.F)
                .monitorCurrent(true)
                .monitorForecast(false)
                .oncePerEvent(true)
                .rearmWindowMinutes(rearmWindowMinutes)
                .build();
    }

    private WeatherData currentAtTemp(double tempC) {
        return WeatherData.builder()
                .id("current-" + tempC)
                .location("Orlando")
                .latitude(28.5383)
                .longitude(-81.3792)
                .eventType("CURRENT_CONDITIONS")
                .temperature(tempC)
                .build();
    }

    private static class InMemoryCriteriaStateRepository implements AlertCriteriaStateRepositoryPort {
        private final Map<String, AlertCriteriaState> states = new HashMap<>();

        @Override
        public Optional<AlertCriteriaState> findByCriteriaId(String criteriaId) {
            return Optional.ofNullable(states.get(criteriaId));
        }

        @Override
        public AlertCriteriaState save(AlertCriteriaState state) {
            states.put(state.getCriteriaId(), state);
            return state;
        }
    }
}
