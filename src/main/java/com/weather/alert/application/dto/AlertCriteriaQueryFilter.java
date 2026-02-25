package com.weather.alert.application.dto;

import com.weather.alert.domain.model.AlertCriteria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertCriteriaQueryFilter {
    private AlertCriteria.TemperatureUnit temperatureUnit;
    private Boolean monitorCurrent;
    private Boolean monitorForecast;
    private Boolean enabled;
    private Boolean hasTemperatureRule;
    private Boolean hasRainRule;
}
