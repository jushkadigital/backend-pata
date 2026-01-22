-- Migration V4.5.0: Remove basePrice from tours and services, add SKU to service combinations
-- Prices are now managed in Vendure e-commerce platform

-- Remove basePrice columns from tours table
ALTER TABLE quarkus.tours
    DROP COLUMN IF EXISTS base_price_amount,
    DROP COLUMN IF EXISTS base_price_currency;

-- Remove basePrice columns from tour_included_services table
ALTER TABLE quarkus.tour_included_services
    DROP COLUMN IF EXISTS base_price_amount,
    DROP COLUMN IF EXISTS base_price_currency;

-- Add SKU column to tour_service_combinations table for Vendure integration
ALTER TABLE quarkus.tour_service_combinations
    ADD COLUMN IF NOT EXISTS sku VARCHAR(50);

-- Update existing combinations with a generated SKU (combination of tour code and combination name)
UPDATE quarkus.tour_service_combinations tsc
SET sku = CONCAT(
    (SELECT t.code FROM quarkus.tours t WHERE t.id = tsc.tour_id),
    '-',
    UPPER(REPLACE(tsc.name, ' ', '-'))
)
WHERE sku IS NULL;

-- Make SKU non-nullable after populating existing records
ALTER TABLE quarkus.tour_service_combinations
    ALTER COLUMN sku SET NOT NULL;

-- Add unique constraint on SKU (globally unique for Vendure)
CREATE UNIQUE INDEX IF NOT EXISTS idx_tour_service_combinations_sku
    ON quarkus.tour_service_combinations(sku);
