ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS event_key VARCHAR(512),
    ADD COLUMN IF NOT EXISTS reason VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS condition_source VARCHAR(64),
    ADD COLUMN IF NOT EXISTS condition_onset TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS condition_expires TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS condition_temperature_c DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS condition_precipitation_probability DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS condition_precipitation_amount DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS expired_at TIMESTAMP WITH TIME ZONE;

UPDATE alerts
SET event_key = COALESCE(event_key, id)
WHERE event_key IS NULL;

ALTER TABLE alerts
    ALTER COLUMN event_key SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_alerts_criteria_event_key_unique ON alerts (criteria_id, event_key);
CREATE INDEX IF NOT EXISTS idx_alerts_criteria_alert_time ON alerts (criteria_id, alert_time DESC);
