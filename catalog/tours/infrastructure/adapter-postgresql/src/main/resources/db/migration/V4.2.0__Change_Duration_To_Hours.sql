-- Change duration from days/nights to hours
ALTER TABLE quarkus.tours DROP COLUMN IF EXISTS duration_days;
ALTER TABLE quarkus.tours DROP COLUMN IF EXISTS duration_nights;
ALTER TABLE quarkus.tours ADD COLUMN IF NOT EXISTS duration_hours INT NOT NULL DEFAULT 8;
