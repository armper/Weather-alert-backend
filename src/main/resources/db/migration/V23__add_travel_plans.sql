CREATE TABLE IF NOT EXISTS travel_plans (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(200),
    destination VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    start_date DATE,
    end_date DATE,
    notes VARCHAR(2000),
    alerts_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_travel_plans_user_id ON travel_plans (user_id);
CREATE INDEX IF NOT EXISTS idx_travel_plans_start_date ON travel_plans (start_date);
