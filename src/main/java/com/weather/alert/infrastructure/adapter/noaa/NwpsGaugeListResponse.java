package com.weather.alert.infrastructure.adapter.noaa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NwpsGaugeListResponse {
    private List<NwpsGauge> gauges;
}
