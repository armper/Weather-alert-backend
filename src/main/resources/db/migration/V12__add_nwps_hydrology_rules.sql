ALTER TABLE alert_criteria
    ADD COLUMN IF NOT EXISTS river_gauge_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS river_stage_threshold DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS river_stage_direction VARCHAR(16),
    ADD COLUMN IF NOT EXISTS river_flood_category_threshold VARCHAR(16);

ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS condition_river_gauge_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS condition_river_observed_stage DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS condition_river_forecast_stage DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS condition_river_flood_stage DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS condition_river_action_stage DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS condition_river_observed_category VARCHAR(32),
    ADD COLUMN IF NOT EXISTS condition_river_forecast_category VARCHAR(32),
    ADD COLUMN IF NOT EXISTS condition_river_stage_unit VARCHAR(16);
