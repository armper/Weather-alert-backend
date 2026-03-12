-- Supports paginated alert history lookups by criteria ordered by time
CREATE INDEX IF NOT EXISTS idx_alerts_criteria_id_alert_time
    ON alerts (criteria_id, alert_time DESC);

-- Supports paginated alert lookups per user ordered by time
CREATE INDEX IF NOT EXISTS idx_alerts_user_id_alert_time
    ON alerts (user_id, alert_time DESC);
