package com.microservice.quarkus.payment.infrastructure.vendure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO representing a product from Vendure GraphQL API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VendureProductDTO(
    String id,
    String name,
    String slug,
    String description,
    boolean enabled,
    String featuredAssetId,
    List<String> assetIds,
    List<VendureVariantDTO> variants
) {}
