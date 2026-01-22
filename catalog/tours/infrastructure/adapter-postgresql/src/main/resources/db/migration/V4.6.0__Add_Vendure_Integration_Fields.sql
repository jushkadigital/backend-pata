-- Add Vendure integration fields to tours and service combinations

-- Add vendure_product_id to tours table
ALTER TABLE quarkus.tours
    ADD COLUMN IF NOT EXISTS vendure_product_id VARCHAR(50);

-- Add Vendure fields to tour_service_combinations table
ALTER TABLE quarkus.tour_service_combinations
    ADD COLUMN IF NOT EXISTS vendure_variant_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS price_amount DECIMAL(19, 2),
    ADD COLUMN IF NOT EXISTS price_currency VARCHAR(3);

-- Create index on vendure_product_id for fast lookups
CREATE INDEX IF NOT EXISTS idx_tours_vendure_product_id
    ON quarkus.tours(vendure_product_id);

-- Create index on vendure_variant_id for fast lookups
CREATE INDEX IF NOT EXISTS idx_tour_combinations_vendure_variant_id
    ON quarkus.tour_service_combinations(vendure_variant_id);
