package com.weather.alert.domain.port;

/**
 * Wrapper for external weather fetch outcomes that distinguishes
 * successful empty data from provider failures.
 */
public record WeatherFetchResult<T>(boolean successful, T data, String failureReason) {

    public static <T> WeatherFetchResult<T> success(T data) {
        return new WeatherFetchResult<>(true, data, null);
    }

    public static <T> WeatherFetchResult<T> failure(T fallbackData, String failureReason) {
        return new WeatherFetchResult<>(false, fallbackData, failureReason);
    }
}
