CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(255),
    name VARCHAR(255),
    email_enabled BOOLEAN,
    sms_enabled BOOLEAN,
    push_enabled BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS alert_criteria (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    radius_km DOUBLE PRECISION,
    event_type VARCHAR(255),
    min_severity VARCHAR(255),
    max_temperature DOUBLE PRECISION,
    min_temperature DOUBLE PRECISION,
    max_wind_speed DOUBLE PRECISION,
    max_precipitation DOUBLE PRECISION,
    enabled BOOLEAN
);

CREATE TABLE IF NOT EXISTS alerts (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    criteria_id VARCHAR(255),
    weather_data_id VARCHAR(255),
    event_type VARCHAR(255),
    severity VARCHAR(255),
    headline VARCHAR(1000),
    description VARCHAR(5000),
    location VARCHAR(255),
    alert_time TIMESTAMP WITH TIME ZONE,
    status VARCHAR(255),
    sent_at TIMESTAMP WITH TIME ZONE,
    acknowledged_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_alerts_user_id ON alerts (user_id);
CREATE INDEX IF NOT EXISTS idx_alerts_status ON alerts (status);
CREATE INDEX IF NOT EXISTS idx_alert_criteria_user_id ON alert_criteria (user_id);
CREATE INDEX IF NOT EXISTS idx_alert_criteria_enabled ON alert_criteria (enabled);
