package com.microservice.quarkus.payment.application.dto;

/**
 * Query DTO for price lookup operations.
 */
public record PriceQuery(
    String productId,
    String variantId,
    int quantity
) {
    public PriceQuery(String productId) {
        this(productId, null, 1);
    }

    public PriceQuery(String productId, int quantity) {
        this(productId, null, quantity);
    }

    public boolean hasVariant() {
        return variantId != null && !variantId.isBlank();
    }
}
