package com.microservice.quarkus.payment.infrastructure.vendure.acl.mapper;

import com.microservice.quarkus.payment.domain.Product;
import com.microservice.quarkus.payment.domain.ProductId;
import com.microservice.quarkus.payment.domain.ProductVariant;
import com.microservice.quarkus.payment.infrastructure.vendure.dto.VendureProductDTO;
import jakarta.inject.Inject;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Maps Vendure product DTOs to domain Product.
 * Part of the Anti-Corruption Layer.
 */
@Mapper(componentModel = "cdi", uses = VendureVariantMapper.class)
public abstract class VendureProductMapper {

    @Inject
    VendureVariantMapper variantMapper;

    /**
     * Converts Vendure product DTO to domain Product.
     */
    public Product toDomain(VendureProductDTO dto) {
        if (dto == null) {
            return null;
        }

        List<ProductVariant> variants = dto.variants() != null
            ? dto.variants().stream()
                .map(variantMapper::toDomain)
                .toList()
            : List.of();

        return new Product(
            ProductId.of(dto.id()),
            dto.name(),
            dto.slug(),
            dto.description(),
            dto.enabled(),
            dto.featuredAssetId(),
            dto.assetIds() != null ? dto.assetIds() : List.of(),
            variants,
            null // syncedAt will be set to now by Product constructor
        );
    }

    /**
     * Converts list of Vendure product DTOs to domain Products.
     */
    public List<Product> toDomainList(List<VendureProductDTO> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
            .map(this::toDomain)
            .toList();
    }
}
