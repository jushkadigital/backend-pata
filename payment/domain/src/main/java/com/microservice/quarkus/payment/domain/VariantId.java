package com.microservice.quarkus.payment.domain;

/**
 * Value object representing a Vendure product variant identifier.
 * In Vendure, variants are the actual sellable items with prices.
 */
public record VariantId(String value) {

    public VariantId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Variant ID cannot be null or blank");
        }
    }

    public static VariantId of(String value) {
        return new VariantId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
