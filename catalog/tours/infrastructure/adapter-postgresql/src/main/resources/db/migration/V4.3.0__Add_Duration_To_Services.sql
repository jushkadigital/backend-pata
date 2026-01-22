-- Add duration_hours column to tour_included_services table
ALTER TABLE quarkus.tour_included_services
    ADD COLUMN IF NOT EXISTS duration_hours INT;
