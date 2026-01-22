-- Payment Outbox Events table for Transactional Outbox Pattern
CREATE TABLE IF NOT EXISTS quarkus.payment_outbox_events (
    id UUID PRIMARY KEY,
    subdomain VARCHAR(50) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_payload TEXT NOT NULL,
    occurred_on TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    published BOOLEAN NOT NULL DEFAULT FALSE
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_payment_outbox_published ON quarkus.payment_outbox_events(published);
CREATE INDEX IF NOT EXISTS idx_payment_outbox_subdomain ON quarkus.payment_outbox_events(subdomain);
CREATE INDEX IF NOT EXISTS idx_payment_outbox_occurred ON quarkus.payment_outbox_events(occurred_on);
