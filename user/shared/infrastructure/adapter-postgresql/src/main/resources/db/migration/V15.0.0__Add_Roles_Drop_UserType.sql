-- Add roles column as VARCHAR (comma-separated: "ADMIN,PASSENGER")
ALTER TABLE quarkus.users ADD COLUMN roles VARCHAR(255);

-- Backfill from user_type (no production data, but be safe)
UPDATE quarkus.users SET roles = LOWER(user_type) WHERE roles IS NULL;

-- Make roles NOT NULL
ALTER TABLE quarkus.users ALTER COLUMN roles SET NOT NULL;

-- Drop old user_type column
ALTER TABLE quarkus.users DROP COLUMN user_type;
