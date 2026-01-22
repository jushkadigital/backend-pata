-- Create shared outbox_events table for User Bounded Context
-- This table stores events from all subdomains: IAM, Admin, Passenger

CREATE TABLE IF NOT EXISTS quarkus.user_outbox_events (
    id UUID PRIMARY KEY,
    subdomain VARCHAR(50) NOT NULL,          -- 'IAM', 'Admin', 'Passenger'
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_payload TEXT NOT NULL,
    occurred_on TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    published BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX IF NOT EXISTS idx_user_outbox_published ON quarkus.user_outbox_events(published);
CREATE INDEX IF NOT EXISTS idx_user_outbox_subdomain ON quarkus.user_outbox_events(subdomain);
CREATE INDEX IF NOT EXISTS idx_user_outbox_occurred ON quarkus.user_outbox_events(occurred_on);
