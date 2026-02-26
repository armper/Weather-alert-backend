CREATE TABLE IF NOT EXISTS user_notification_preferences (
    user_id VARCHAR(255) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    enabled_channels VARCHAR(255) NOT NULL DEFAULT 'EMAIL',
    preferred_channel VARCHAR(32) NOT NULL DEFAULT 'EMAIL',
    fallback_strategy VARCHAR(32) NOT NULL DEFAULT 'FIRST_SUCCESS',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS criteria_notification_preferences (
    criteria_id VARCHAR(255) PRIMARY KEY REFERENCES alert_criteria(id) ON DELETE CASCADE,
    use_user_defaults BOOLEAN NOT NULL DEFAULT TRUE,
    enabled_channels VARCHAR(255),
    preferred_channel VARCHAR(32),
    fallback_strategy VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS channel_verifications (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel VARCHAR(32) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    verification_token_hash VARCHAR(255),
    token_expires_at TIMESTAMP WITH TIME ZONE,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS alert_delivery (
    id VARCHAR(255) PRIMARY KEY,
    alert_id VARCHAR(255) NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(2000),
    provider_message_id VARCHAR(255),
    sent_at TIMESTAMP WITH TIME ZONE,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_channel_verifications_user_channel_destination_unique
    ON channel_verifications (user_id, channel, destination);
CREATE UNIQUE INDEX IF NOT EXISTS idx_alert_delivery_alert_channel_unique
    ON alert_delivery (alert_id, channel);
CREATE INDEX IF NOT EXISTS idx_alert_delivery_status_next_attempt
    ON alert_delivery (status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_alert_delivery_user_created_at
    ON alert_delivery (user_id, created_at DESC);
