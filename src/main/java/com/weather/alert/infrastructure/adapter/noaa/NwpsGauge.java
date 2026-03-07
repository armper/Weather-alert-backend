package com.weather.alert.infrastructure.adapter.noaa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NwpsGauge {
    private String lid;
    private String usgsId;
    private String name;
    private String description;
    private String county;
    private Double latitude;
    private Double longitude;
    private Reference rfc;
    private Reference wfo;
    private Reference state;
    private Status status;
    private Flood flood;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Reference {
        private String abbreviation;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        private StatusValue observed;
        private StatusValue forecast;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusValue {
        private Double primary;
        private String primaryUnit;
        private Double secondary;
        private String secondaryUnit;
        private String floodCategory;
        private String validTime;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Flood {
        private String stageUnits;
        private String flowUnits;
        private FloodCategories categories;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FloodCategories {
        private FloodCategoryValue action;
        private FloodCategoryValue minor;
        private FloodCategoryValue moderate;
        private FloodCategoryValue major;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FloodCategoryValue {
        private Double stage;
        private Double flow;
    }
}
