package com.microservice.quarkus.payment.infrastructure.vendure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing a product variant from Vendure GraphQL API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VendureVariantDTO(
    String id,
    String sku,
    String name,
    int priceWithTax,
    String currencyCode,
    boolean enabled,
    int stockLevel
) {}
