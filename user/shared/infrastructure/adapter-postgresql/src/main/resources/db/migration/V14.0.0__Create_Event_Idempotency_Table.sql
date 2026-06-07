CREATE TABLE IF NOT EXISTS quarkus.event_idempotency (
    event_id VARCHAR(255) NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    CONSTRAINT pk_event_idempotency PRIMARY KEY (event_id, consumer_group)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_expires ON quarkus.event_idempotency(expires_at);
