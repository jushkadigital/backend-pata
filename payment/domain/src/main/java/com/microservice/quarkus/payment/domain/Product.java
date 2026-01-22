package com.microservice.quarkus.payment.domain;

import java.time.Instant;
import java.util.List;

/**
 * Immutable record representing a product synced from Vendure.
 * This is a read model - the source of truth is Vendure.
 */
public record Product(
    ProductId id,
    String name,
    String slug,
    String description,
    boolean enabled,
    String featuredAssetId,
    List<String> assetIds,
    List<ProductVariant> variants,
    Instant syncedAt
) {

    public Product {
        if (id == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Product slug cannot be null or blank");
        }
        if (assetIds == null) {
            assetIds = List.of();
        } else {
            assetIds = List.copyOf(assetIds);
        }
        if (variants == null) {
            variants = List.of();
        } else {
            variants = List.copyOf(variants);
        }
        if (syncedAt == null) {
            syncedAt = Instant.now();
        }
    }

    public static Product of(
        String id,
        String name,
        String slug,
        String description,
        boolean enabled,
        String featuredAssetId,
        List<String> assetIds,
        List<ProductVariant> variants
    ) {
        return new Product(
            ProductId.of(id),
            name,
            slug,
            description,
            enabled,
            featuredAssetId,
            assetIds,
            variants,
            Instant.now()
        );
    }

    public boolean hasVariants() {
        return !variants.isEmpty();
    }

    public ProductVariant defaultVariant() {
        if (variants.isEmpty()) {
            throw new IllegalStateException("Product has no variants");
        }
        return variants.get(0);
    }
}
