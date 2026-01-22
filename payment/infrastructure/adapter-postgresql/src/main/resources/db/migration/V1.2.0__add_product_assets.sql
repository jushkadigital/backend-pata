-- Add asset columns to products table
ALTER TABLE quarkus.products ADD COLUMN IF NOT EXISTS featured_asset_id VARCHAR(255);

-- Create junction table for product assets
CREATE TABLE IF NOT EXISTS quarkus.product_assets (
    product_id VARCHAR(255) NOT NULL,
    asset_id VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id, asset_id),
    CONSTRAINT fk_product_assets_product
        FOREIGN KEY (product_id)
        REFERENCES quarkus.products(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_product_assets_product_id
    ON quarkus.product_assets(product_id);
