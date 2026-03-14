ALTER TABLE travel_plans
    ADD COLUMN IF NOT EXISTS alert_coverage_mode VARCHAR(32) NOT NULL DEFAULT 'ALL_ALERTS';

ALTER TABLE travel_plans
    ADD COLUMN IF NOT EXISTS selected_alert_topics TEXT;

ALTER TABLE travel_plans
    ADD COLUMN IF NOT EXISTS linked_criteria_ids TEXT;
