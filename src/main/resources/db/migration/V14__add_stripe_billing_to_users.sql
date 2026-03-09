ALTER TABLE users
    ADD COLUMN stripe_customer_id VARCHAR(255),
    ADD COLUMN stripe_subscription_id VARCHAR(255),
    ADD COLUMN stripe_price_id VARCHAR(255),
    ADD COLUMN stripe_subscription_status VARCHAR(64),
    ADD COLUMN stripe_current_period_end TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_stripe_customer_id
    ON users (stripe_customer_id)
    WHERE stripe_customer_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_stripe_subscription_id
    ON users (stripe_subscription_id)
    WHERE stripe_subscription_id IS NOT NULL;
