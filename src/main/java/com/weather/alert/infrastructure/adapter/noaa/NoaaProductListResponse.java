package com.weather.alert.infrastructure.adapter.noaa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class NoaaProductListResponse {
    @JsonProperty("@graph")
    private List<NoaaProductItem> graph;
}
