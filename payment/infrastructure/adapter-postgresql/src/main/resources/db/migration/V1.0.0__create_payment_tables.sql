-- Payment module: Products synced from Vendure

CREATE TABLE IF NOT EXISTS quarkus.products (
    id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT true,
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_products_slug UNIQUE (slug)
);

CREATE TABLE IF NOT EXISTS quarkus.product_variants (
    id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    sku VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    price_amount DECIMAL(10, 2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    stock_level INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES quarkus.products(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_products_slug ON quarkus.products(slug);
CREATE INDEX IF NOT EXISTS idx_products_enabled ON quarkus.products(enabled);
CREATE INDEX IF NOT EXISTS idx_product_variants_product_id ON quarkus.product_variants(product_id);
CREATE INDEX IF NOT EXISTS idx_product_variants_sku ON quarkus.product_variants(sku);
