-- Index to speed up retention cleanup of old alert_delivery rows by created_at.
CREATE INDEX IF NOT EXISTS idx_alert_delivery_created_at
    ON alert_delivery (created_at);
