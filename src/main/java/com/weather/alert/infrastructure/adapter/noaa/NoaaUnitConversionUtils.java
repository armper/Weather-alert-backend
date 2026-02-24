package com.weather.alert.infrastructure.adapter.noaa;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NoaaUnitConversionUtils {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(-?\\d+(?:\\.\\d+)?)");
    private static final double MPH_TO_KMH = 1.60934;
    private static final double KNOT_TO_KMH = 1.852;

    private NoaaUnitConversionUtils() {
    }

    static Double toCelsius(Double value, String unitCodeOrSymbol) {
        if (value == null) {
            return null;
        }
        String unit = normalize(unitCodeOrSymbol);
        if ("degc".equals(unit) || "c".equals(unit) || "celsius".equals(unit)) {
            return value;
        }
        if ("degf".equals(unit) || "f".equals(unit) || "fahrenheit".equals(unit)) {
            return (value - 32.0) * 5.0 / 9.0;
        }
        return value;
    }

    static Double toKilometersPerHour(Double value, String unitCode) {
        if (value == null) {
            return null;
        }
        String unit = normalize(unitCode);
        if ("km_h-1".equals(unit) || "kmh".equals(unit)) {
            return value;
        }
        if ("m_s-1".equals(unit)) {
            return value * 3.6;
        }
        if ("mi_h-1".equals(unit) || "mph".equals(unit)) {
            return value * MPH_TO_KMH;
        }
        if ("kt".equals(unit)) {
            return value * KNOT_TO_KMH;
        }
        return value;
    }

    static Double parseWindSpeedToKmh(String windSpeedText) {
        if (windSpeedText == null || windSpeedText.isBlank()) {
            return null;
        }
        double max = Double.NaN;
        Matcher matcher = NUMBER_PATTERN.matcher(windSpeedText);
        while (matcher.find()) {
            double value = Double.parseDouble(matcher.group(1));
            if (Double.isNaN(max) || value > max) {
                max = value;
            }
        }
        if (Double.isNaN(max)) {
            return null;
        }

        String text = windSpeedText.toLowerCase();
        if (text.contains("mph")) {
            return max * MPH_TO_KMH;
        }
        if (text.contains("km/h")) {
            return max;
        }
        if (text.contains("kt")) {
            return max * KNOT_TO_KMH;
        }
        return max;
    }

    static Double toMillimeters(Double value, String unitCode) {
        if (value == null) {
            return null;
        }
        String unit = normalize(unitCode);
        if ("mm".equals(unit)) {
            return value;
        }
        if ("m".equals(unit)) {
            return value * 1000.0;
        }
        if ("in".equals(unit)) {
            return value * 25.4;
        }
        return value;
    }

    private static String normalize(String unitCode) {
        if (unitCode == null) {
            return "";
        }
        String normalized = unitCode.toLowerCase();
        int colon = normalized.indexOf(':');
        if (colon >= 0 && colon + 1 < normalized.length()) {
            normalized = normalized.substring(colon + 1);
        }
        return normalized.trim();
    }
}
