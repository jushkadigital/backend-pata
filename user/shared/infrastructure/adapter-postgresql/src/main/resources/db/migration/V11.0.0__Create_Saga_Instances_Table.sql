-- Create saga_instances table for User Bounded Context
CREATE TABLE IF NOT EXISTS quarkus.user_saga_instances (
    id UUID PRIMARY KEY,
    saga_type VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    current_step VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_saga_correlation_id ON quarkus.user_saga_instances(correlation_id);
CREATE INDEX IF NOT EXISTS idx_saga_status ON quarkus.user_saga_instances(status);
