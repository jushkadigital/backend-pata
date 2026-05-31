-- Drop subdomain column (replaced by producer)
ALTER TABLE quarkus.user_outbox_events DROP COLUMN IF EXISTS subdomain;

-- Add missing event metadata columns
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS causation_id VARCHAR(255);
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64);
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS span_id VARCHAR(64);
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS producer VARCHAR(100) NOT NULL DEFAULT 'unknown';
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS actor_id VARCHAR(255);
ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255);

-- Index for causation lookups
CREATE INDEX IF NOT EXISTS idx_outbox_causation_id ON quarkus.user_outbox_events(causation_id);
-- Index for trace correlation
CREATE INDEX IF NOT EXISTS idx_outbox_trace_id ON quarkus.user_outbox_events(trace_id);
-- Index for producer queries
CREATE INDEX IF NOT EXISTS idx_outbox_producer ON quarkus.user_outbox_events(producer);
