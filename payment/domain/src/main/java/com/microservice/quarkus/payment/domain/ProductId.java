package com.microservice.quarkus.payment.domain;

/**
 * Value object representing a Vendure product identifier.
 * Wraps the external Vendure ID for type safety.
 */
public record ProductId(String value) {

    public ProductId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or blank");
        }
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
