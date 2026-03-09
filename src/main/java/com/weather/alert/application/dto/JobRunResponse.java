package com.weather.alert.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class JobRunResponse {

    private String jobName;
    private String status;
    private Instant startedAt;
    private Instant finishedAt;
    private long durationMillis;
    private String message;
    private Map<String, Long> metrics;
}
