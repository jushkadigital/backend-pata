-- Add correlation, versioning, retry, DLQ columns to outbox table
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS event_version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(255);
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS max_retries INTEGER NOT NULL DEFAULT 5;
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP;
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS dead BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS dead_reason TEXT;
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS dead_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_outbox_correlation_id ON quarkus.user_outbox_events(correlation_id);
CREATE INDEX IF NOT EXISTS idx_outbox_dead ON quarkus.user_outbox_events(dead) WHERE dead = true;
CREATE INDEX IF NOT EXISTS idx_outbox_next_retry ON quarkus.user_outbox_events(next_retry_at) WHERE published = false AND dead = false;
