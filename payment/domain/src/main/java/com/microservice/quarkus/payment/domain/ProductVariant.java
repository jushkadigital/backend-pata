package com.microservice.quarkus.payment.domain;

/**
 * Immutable record representing a product variant from Vendure.
 * Variants are the actual sellable items with prices and stock.
 */
public record ProductVariant(
    VariantId id,
    String sku,
    String name,
    Money price,
    boolean enabled,
    int stockLevel
) {

    public ProductVariant {
        if (id == null) {
            throw new IllegalArgumentException("Variant ID cannot be null");
        }
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank");
        }
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        if (stockLevel < 0) {
            stockLevel = 0;
        }
    }

    public static ProductVariant of(
        String id,
        String sku,
        String name,
        Money price,
        boolean enabled,
        int stockLevel
    ) {
        return new ProductVariant(
            VariantId.of(id),
            sku,
            name,
            price,
            enabled,
            stockLevel
        );
    }

    public boolean isInStock() {
        return stockLevel > 0;
    }

    public boolean isAvailable() {
        return enabled && isInStock();
    }
}
