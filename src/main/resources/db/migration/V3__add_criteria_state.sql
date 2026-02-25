CREATE TABLE IF NOT EXISTS criteria_state (
    criteria_id VARCHAR(255) PRIMARY KEY,
    last_condition_met BOOLEAN NOT NULL DEFAULT FALSE,
    last_event_signature VARCHAR(512),
    last_notified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_criteria_state_last_condition_met ON criteria_state (last_condition_met);
CREATE INDEX IF NOT EXISTS idx_criteria_state_last_notified_at ON criteria_state (last_notified_at);
CREATE INDEX IF NOT EXISTS idx_criteria_state_updated_at ON criteria_state (updated_at);
