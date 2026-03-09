package com.weather.alert.infrastructure.config;

import com.weather.alert.application.usecase.RunWeatherAlertProcessingUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WeatherAlertSchedulerTest {

    @Mock
    private RunWeatherAlertProcessingUseCase runWeatherAlertProcessingUseCase;

    @Test
    void shouldDelegateToUseCase() {
        WeatherAlertScheduler scheduler = new WeatherAlertScheduler(runWeatherAlertProcessingUseCase);

        scheduler.processWeatherAlerts();

        verify(runWeatherAlertProcessingUseCase).run();
    }
}
