package com.weather.alert.domain.service.evaluation;

import com.weather.alert.domain.model.AlertCriteria;
import com.weather.alert.domain.model.WeatherData;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Explicit rule engine for evaluating an {@link AlertCriteria} against weather data.
 * Filter rules must all pass. Trigger rules are OR'ed.
 */
@Component
public class AlertCriteriaRuleEvaluator {

    private static final AlertCriteriaRuleEvaluator DEFAULT_INSTANCE = new AlertCriteriaRuleEvaluator();

    private final List<CriteriaRule> filterRules;
    private final List<CriteriaRule> triggerRules;

    public AlertCriteriaRuleEvaluator() {
        this.filterRules = List.of(
                new LocationRule(),
                new EventTypeRule(),
                new SeverityRule()
        );
        this.triggerRules = List.of(
                new TemperatureThresholdRule(),
                new LegacyTemperatureRangeRule(),
                new HumidityThresholdRule(),
                new DewPointThresholdRule(),
                new WindSpeedRule(),
                new WindGustThresholdRule(),
                new SkyCoverThresholdRule(),
                new RiverStageThresholdRule(),
                new RiverFloodCategoryRule(),
                new LegacyPrecipitationRule(),
                new RainThresholdRule()
        );
    }

    public static AlertCriteriaRuleEvaluator defaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
        if (criteria == null || weatherData == null) {
            return false;
        }
        if (!Boolean.TRUE.equals(criteria.getEnabled())) {
            return false;
        }

        boolean hasAnyFilterRule = false;
        for (CriteriaRule rule : filterRules) {
            if (!rule.applies(criteria)) {
                continue;
            }
            hasAnyFilterRule = true;
            if (!rule.matches(criteria, weatherData)) {
                return false;
            }
        }

        boolean hasAnyTriggerRule = false;
        for (CriteriaRule rule : triggerRules) {
            if (!rule.applies(criteria)) {
                continue;
            }
            hasAnyTriggerRule = true;
            if (rule.matches(criteria, weatherData)) {
                return true;
            }
        }

        return !hasAnyTriggerRule && hasAnyFilterRule;
    }

    public boolean hasWeatherConditionRules(AlertCriteria criteria) {
        if (criteria == null) {
            return false;
        }
        return criteria.getTemperatureThreshold() != null
                || criteria.getMaxTemperature() != null
                || criteria.getMinTemperature() != null
                || criteria.getMaxWindSpeed() != null
                || criteria.getHumidityThreshold() != null
                || criteria.getDewPointThreshold() != null
                || criteria.getWindGustThreshold() != null
                || criteria.getSkyCoverThreshold() != null
                || criteria.getMaxPrecipitation() != null
                || criteria.getRainThreshold() != null;
    }

    public boolean hasHydrologyConditionRules(AlertCriteria criteria) {
        if (criteria == null) {
            return false;
        }
        return criteria.getRiverStageThreshold() != null
                || criteria.getRiverFloodCategoryThreshold() != null;
    }

    private interface CriteriaRule {
        boolean applies(AlertCriteria criteria);

        boolean matches(AlertCriteria criteria, WeatherData weatherData);
    }

    private static final class LocationRule implements CriteriaRule {

        @Override
        public boolean applies(AlertCriteria criteria) {
            return hasText(criteria.getLocation()) || hasCoordinateRadius(criteria);
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            boolean locationConfigured = hasText(criteria.getLocation());
            boolean coordinateConfigured = hasCoordinateRadius(criteria);

            boolean locationMatched = !locationConfigured || matchesLocationText(criteria, weatherData);
            boolean coordinateMatched = !coordinateConfigured || matchesCoordinates(criteria, weatherData);

            if (locationConfigured && coordinateConfigured) {
                return locationMatched || coordinateMatched;
            }
            return locationMatched && coordinateMatched;
        }

        private boolean matchesLocationText(AlertCriteria criteria, WeatherData weatherData) {
            if (!hasText(weatherData.getLocation())) {
                return false;
            }
            return weatherData.getLocation().toLowerCase(Locale.ROOT)
                    .contains(criteria.getLocation().toLowerCase(Locale.ROOT));
        }

        private boolean matchesCoordinates(AlertCriteria criteria, WeatherData weatherData) {
            if (weatherData.getLatitude() == null || weatherData.getLongitude() == null) {
                return false;
            }
            double distance = calculateDistance(
                    criteria.getLatitude(),
                    criteria.getLongitude(),
                    weatherData.getLatitude(),
                    weatherData.getLongitude()
            );
            return distance <= criteria.getRadiusKm();
        }

        private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
            final double earthRadiusKm = 6371.0;
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return earthRadiusKm * c;
        }
    }

    private static final class EventTypeRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            return hasText(criteria.getEventType());
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            String expected = criteria.getEventType();
            if (equalsIgnoreCase(weatherData.getEventType(), expected)) {
                return true;
            }

            // For condition-based weather data, eventType is synthetic (CURRENT/FORECAST_CONDITIONS).
            // Support user-friendly event filters (e.g., "Rain") by matching headline/description text.
            return containsIgnoreCase(weatherData.getHeadline(), expected)
                    || containsIgnoreCase(weatherData.getDescription(), expected);
        }
    }

    private static final class SeverityRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            return hasText(criteria.getMinSeverity());
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            if (!hasText(weatherData.getSeverity())) {
                return false;
            }
            return getSeverityLevel(weatherData.getSeverity()) >= getSeverityLevel(criteria.getMinSeverity());
        }

        private int getSeverityLevel(String severity) {
            return switch (severity.toUpperCase(Locale.ROOT)) {
                case "EXTREME" -> 4;
                case "SEVERE" -> 3;
                case "MODERATE" -> 2;
                case "MINOR" -> 1;
                default -> 0;
            };
        }
    }

    private static final class TemperatureThresholdRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            return criteria.getTemperatureThreshold() != null && criteria.getTemperatureDirection() != null;
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            if (weatherData.getTemperature() == null) {
                return false;
            }
            double thresholdInCelsius = toCelsius(criteria.getTemperatureThreshold(), criteria.getTemperatureUnit());
            return switch (criteria.getTemperatureDirection()) {
                case ABOVE -> weatherData.getTemperature() > thresholdInCelsius;
                case BELOW -> weatherData.getTemperature() < thresholdInCelsius;
            };
        }

        private double toCelsius(double threshold, AlertCriteria.TemperatureUnit unit) {
            AlertCriteria.TemperatureUnit effectiveUnit =
                    unit == null ? AlertCriteria.TemperatureUnit.F : unit;
            if (effectiveUnit == AlertCriteria.TemperatureUnit.C) {
                return threshold;
            }
            return (threshold - 32.0) * 5.0 / 9.0;
        }
    }

    private static final class LegacyTemperatureRangeRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            // New threshold mode takes precedence when configured, to avoid mixed-mode false positives.
            if (criteria.getTemperatureThreshold() != null || criteria.getTemperatureDirection() != null) {
                return false;
            }
            return criteria.getMaxTemperature() != null || criteria.getMinTemperature() != null;
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            if (weatherData.getTemperature() == null) {
                return false;
            }
            if (criteria.getMaxTemperature() != null && weatherData.getTemperature() > criteria.getMaxTemperature()) {
                return true;
            }
            return criteria.getMinTemperature() != null && weatherData.getTemperature() < criteria.getMinTemperature();
        }
    }

    private static final class WindSpeedRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            return criteria.getMaxWindSpeed() != null;
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            return weatherData.getWindSpeed() != null && weatherData.getWindSpeed() > criteria.getMaxWindSpeed();
        }
    }

    private static final class HumidityThresholdRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            return criteria.getHumidityThreshold() != null && criteria.getHumidityDirection() != null;
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            return compare(
                    weatherData.getHumidity(),
                    criteria.getHumidityThreshold(),
                    criteria.getHumidityDirection());
        }
    }

    private static final class DewPointThresholdRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            return criteria.getDewPointThreshold() != null && criteria.getDewPointDirection() != null;
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            if (weatherData.getDewPoint() == null) {
                return false;
            }
            double thresholdInCelsius = toCelsius(criteria.getDewPointThreshold(), criteria.getTemperatureUnit());
            return compare(weatherData.getDewPoint(), thresholdInCelsius, criteria.getDewPointDirection());
        }
    }

    private static final class LegacyPrecipitationRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            // New rain threshold mode takes precedence when configured.
            if (criteria.getRainThreshold() != null || criteria.getRainThresholdType() != null) {
                return false;
            }
            return criteria.getMaxPrecipitation() != null;
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            Double precipitation = precipitationAmount(weatherData);
            return precipitation != null && precipitation > criteria.getMaxPrecipitation();
        }
    }

    private static final class RainThresholdRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            return criteria.getRainThreshold() != null && criteria.getRainThresholdType() != null;
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            Double measuredValue = switch (criteria.getRainThresholdType()) {
                case PROBABILITY -> precipitationProbability(weatherData);
                case AMOUNT -> precipitationAmount(weatherData);
            };
            return measuredValue != null && measuredValue >= criteria.getRainThreshold();
        }
    }

    private static final class WindGustThresholdRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            return criteria.getWindGustThreshold() != null;
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            return weatherData.getWindGust() != null && weatherData.getWindGust() > criteria.getWindGustThreshold();
        }
    }

    private static final class SkyCoverThresholdRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            return criteria.getSkyCoverThreshold() != null && criteria.getSkyCoverDirection() != null;
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            return compare(
                    weatherData.getSkyCover(),
                    criteria.getSkyCoverThreshold(),
                    criteria.getSkyCoverDirection());
        }
    }

    private static final class RiverStageThresholdRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            return criteria.getRiverStageThreshold() != null && criteria.getRiverStageDirection() != null;
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            Double measuredValue = riverStage(weatherData);
            return compare(measuredValue, criteria.getRiverStageThreshold(), criteria.getRiverStageDirection());
        }
    }

    private static final class RiverFloodCategoryRule implements CriteriaRule {
        @Override
        public boolean applies(AlertCriteria criteria) {
            return criteria.getRiverFloodCategoryThreshold() != null;
        }

        @Override
        public boolean matches(AlertCriteria criteria, WeatherData weatherData) {
            return floodCategoryLevel(resolveRiverFloodCategory(weatherData))
                    >= floodCategoryLevel(criteria.getRiverFloodCategoryThreshold());
        }
    }

    private static boolean hasCoordinateRadius(AlertCriteria criteria) {
        return criteria.getLatitude() != null && criteria.getLongitude() != null && criteria.getRadiusKm() != null;
    }

    private static Double precipitationAmount(WeatherData weatherData) {
        if (weatherData.getPrecipitationAmount() != null) {
            return weatherData.getPrecipitationAmount();
        }
        return weatherData.getPrecipitation();
    }

    private static Double precipitationProbability(WeatherData weatherData) {
        if (weatherData.getPrecipitationProbability() != null) {
            return weatherData.getPrecipitationProbability();
        }
        return weatherData.getPrecipitation();
    }

    private static Double riverStage(WeatherData weatherData) {
        if (weatherData == null) {
            return null;
        }
        String eventType = weatherData.getEventType();
        if ("RIVER_FORECAST_CONDITIONS".equalsIgnoreCase(eventType)) {
            return weatherData.getRiverForecastStage();
        }
        return weatherData.getRiverObservedStage();
    }

    private static String resolveRiverFloodCategory(WeatherData weatherData) {
        if (weatherData == null) {
            return null;
        }
        String eventType = weatherData.getEventType();
        if ("RIVER_FORECAST_CONDITIONS".equalsIgnoreCase(eventType)) {
            return weatherData.getRiverForecastCategory();
        }
        return weatherData.getRiverObservedCategory();
    }

    private static int floodCategoryLevel(String category) {
        if (!hasText(category)) {
            return 0;
        }
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "major" -> 4;
            case "moderate" -> 3;
            case "minor" -> 2;
            case "action" -> 1;
            default -> 0;
        };
    }

    private static int floodCategoryLevel(AlertCriteria.FloodCategory category) {
        if (category == null) {
            return 0;
        }
        return switch (category) {
            case ACTION -> 1;
            case MINOR -> 2;
            case MODERATE -> 3;
            case MAJOR -> 4;
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private static boolean containsIgnoreCase(String text, String value) {
        if (!hasText(text) || !hasText(value)) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
    }

    private static boolean compare(Double measuredValue, Double threshold, AlertCriteria.ComparisonDirection direction) {
        if (measuredValue == null || threshold == null || direction == null) {
            return false;
        }
        return switch (direction) {
            case ABOVE -> measuredValue > threshold;
            case BELOW -> measuredValue < threshold;
        };
    }

    private static double toCelsius(double threshold, AlertCriteria.TemperatureUnit unit) {
        AlertCriteria.TemperatureUnit effectiveUnit = unit == null ? AlertCriteria.TemperatureUnit.F : unit;
        if (effectiveUnit == AlertCriteria.TemperatureUnit.C) {
            return threshold;
        }
        return (threshold - 32.0) * 5.0 / 9.0;
    }
}
