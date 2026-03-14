CREATE TABLE IF NOT EXISTS travel_plans (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(120) NOT NULL,
    destination VARCHAR(180) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    notes VARCHAR(500),
    alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_travel_plans_user_id ON travel_plans (user_id);
CREATE INDEX IF NOT EXISTS idx_travel_plans_user_start_date ON travel_plans (user_id, start_date);
