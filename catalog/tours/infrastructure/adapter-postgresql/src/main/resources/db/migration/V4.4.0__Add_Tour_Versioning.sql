-- Add versioning support for tours
ALTER TABLE quarkus.tours
    ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 1;

ALTER TABLE quarkus.tours
    ADD COLUMN IF NOT EXISTS parent_tour_id VARCHAR(36);

-- Drop the old unique index on code (if exists)
DROP INDEX IF EXISTS quarkus.idx_tours_code;

-- Create new unique index on code + version (allows multiple versions of same tour code)
CREATE UNIQUE INDEX IF NOT EXISTS idx_tours_code_version ON quarkus.tours(code, version);

-- Create index for parent tour lookup
CREATE INDEX IF NOT EXISTS idx_tours_parent ON quarkus.tours(parent_tour_id);

-- Re-create non-unique index on code for lookups
CREATE INDEX IF NOT EXISTS idx_tours_code ON quarkus.tours(code);
