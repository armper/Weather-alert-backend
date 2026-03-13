package com.weather.alert.infrastructure.adapter.noaa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class NoaaAlertGeocode {
    @JsonProperty("SAME")
    private List<String> same;

    @JsonProperty("UGC")
    private List<String> ugc;
}
