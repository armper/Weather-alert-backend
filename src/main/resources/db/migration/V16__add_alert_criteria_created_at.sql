ALTER TABLE alert_criteria
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE;

UPDATE alert_criteria
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

ALTER TABLE alert_criteria
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE alert_criteria
    ALTER COLUMN created_at SET NOT NULL;
