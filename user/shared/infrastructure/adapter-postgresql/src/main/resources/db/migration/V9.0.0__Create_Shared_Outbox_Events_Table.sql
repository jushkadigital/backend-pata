-- Create shared outbox_events table for User Bounded Context
-- This table stores events from all subdomains: IAM, Admin, Passenger
ALTER TABLE quarkus.user_outbox_events
ADD COLUMN scope VARCHAR(20) DEFAULT 'INTERNAL_ONLY' NOT NULL;
