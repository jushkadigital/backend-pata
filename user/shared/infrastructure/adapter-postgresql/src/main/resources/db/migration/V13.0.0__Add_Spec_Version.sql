ALTER TABLE quarkus.user_outbox_events ADD COLUMN IF NOT EXISTS spec_version VARCHAR(50);
