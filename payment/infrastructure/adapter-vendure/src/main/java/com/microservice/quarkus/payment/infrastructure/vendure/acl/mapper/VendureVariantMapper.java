package com.microservice.quarkus.payment.infrastructure.vendure.acl.mapper;

import com.microservice.quarkus.payment.domain.Money;
import com.microservice.quarkus.payment.domain.ProductVariant;
import com.microservice.quarkus.payment.domain.VariantId;
import com.microservice.quarkus.payment.infrastructure.vendure.dto.VendureVariantDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps Vendure variant DTOs to domain ProductVariant.
 * Part of the Anti-Corruption Layer.
 */
@Mapper(componentModel = "cdi")
public interface VendureVariantMapper {

    /**
     * Converts Vendure variant to domain variant.
     * Note: priceWithTax is in minor units (cents), needs conversion.
     */
    default ProductVariant toDomain(VendureVariantDTO dto) {
        if (dto == null) {
            return null;
        }

        // Vendure returns price in minor units (cents)
        Money price = Money.of(dto.priceWithTax(), dto.currencyCode());

        return new ProductVariant(
            VariantId.of(dto.id()),
            dto.sku(),
            dto.name(),
            price,
            dto.enabled(),
            dto.stockLevel()
        );
    }
}
