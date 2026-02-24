ALTER TABLE alert_criteria
    ADD COLUMN IF NOT EXISTS temperature_threshold DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS temperature_direction VARCHAR(16),
    ADD COLUMN IF NOT EXISTS rain_threshold DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS rain_threshold_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS monitor_current BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS monitor_forecast BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS forecast_window_hours INTEGER NOT NULL DEFAULT 48,
    ADD COLUMN IF NOT EXISTS temperature_unit VARCHAR(4) NOT NULL DEFAULT 'F',
    ADD COLUMN IF NOT EXISTS once_per_event BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS rearm_window_minutes INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_alert_criteria_monitor_current ON alert_criteria (monitor_current);
CREATE INDEX IF NOT EXISTS idx_alert_criteria_monitor_forecast ON alert_criteria (monitor_forecast);
