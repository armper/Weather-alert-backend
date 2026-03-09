CREATE TABLE IF NOT EXISTS weather_data (
    id VARCHAR(255) PRIMARY KEY,
    location VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    event_type VARCHAR(255),
    severity VARCHAR(255),
    headline VARCHAR(1000),
    description VARCHAR(5000),
    onset TIMESTAMP WITH TIME ZONE,
    expires TIMESTAMP WITH TIME ZONE,
    status VARCHAR(255),
    message_type VARCHAR(255),
    category VARCHAR(255),
    urgency VARCHAR(255),
    certainty VARCHAR(255),
    temperature DOUBLE PRECISION,
    wind_speed DOUBLE PRECISION,
    precipitation DOUBLE PRECISION,
    precipitation_probability DOUBLE PRECISION,
    precipitation_amount DOUBLE PRECISION,
    humidity DOUBLE PRECISION,
    dew_point DOUBLE PRECISION,
    wind_gust DOUBLE PRECISION,
    sky_cover DOUBLE PRECISION,
    river_gauge_id VARCHAR(255),
    river_observed_stage DOUBLE PRECISION,
    river_forecast_stage DOUBLE PRECISION,
    river_flood_stage DOUBLE PRECISION,
    river_action_stage DOUBLE PRECISION,
    river_observed_category VARCHAR(255),
    river_forecast_category VARCHAR(255),
    river_stage_unit VARCHAR(32),
    river_distance_km DOUBLE PRECISION,
    recorded_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_weather_data_recorded_at
    ON weather_data (recorded_at DESC);

CREATE INDEX IF NOT EXISTS idx_weather_data_event_type
    ON weather_data (event_type);

CREATE INDEX IF NOT EXISTS idx_weather_data_severity
    ON weather_data (severity);

CREATE INDEX IF NOT EXISTS idx_weather_data_location
    ON weather_data (location);
