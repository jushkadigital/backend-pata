package com.microservice.quarkus.payment.infrastructure.postgresql;

import com.microservice.quarkus.payment.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

@Mapper(componentModel = "cdi")
public interface ProductMapper {

    default Product toDomain(ProductEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Product(
            ProductId.of(entity.getId()),
            entity.getName(),
            entity.getSlug(),
            entity.getDescription(),
            entity.isEnabled(),
            entity.getFeaturedAssetId(),
            entity.getAssetIds() != null ? List.copyOf(entity.getAssetIds()) : List.of(),
            toVariantDomainList(entity.getVariants()),
            entity.getSyncedAt()
        );
    }

    default List<Product> toDomainList(List<ProductEntity> entities) {
        return entities.stream().map(this::toDomain).toList();
    }

    default ProductVariant toVariantDomain(ProductVariantEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProductVariant(
            VariantId.of(entity.getId()),
            entity.getSku(),
            entity.getName(),
            new Money(entity.getPriceAmount(), Currency.getInstance(entity.getPriceCurrency())),
            entity.isEnabled(),
            entity.getStockLevel()
        );
    }

    default List<ProductVariant> toVariantDomainList(List<ProductVariantEntity> entities) {
        return entities.stream().map(this::toVariantDomain).toList();
    }

    default ProductEntity toEntity(Product product) {
        if (product == null) {
            return null;
        }
        ProductEntity entity = new ProductEntity();
        entity.setId(product.id().value());
        entity.setName(product.name());
        entity.setSlug(product.slug());
        entity.setDescription(product.description());
        entity.setEnabled(product.enabled());
        entity.setFeaturedAssetId(product.featuredAssetId());
        entity.setAssetIds(product.assetIds() != null ? new ArrayList<>(product.assetIds()) : new ArrayList<>());
        entity.setSyncedAt(product.syncedAt());

        for (ProductVariant variant : product.variants()) {
            ProductVariantEntity variantEntity = toVariantEntity(variant);
            entity.addVariant(variantEntity);
        }

        return entity;
    }

    default ProductVariantEntity toVariantEntity(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        ProductVariantEntity entity = new ProductVariantEntity();
        entity.setId(variant.id().value());
        entity.setSku(variant.sku());
        entity.setName(variant.name());
        entity.setPriceAmount(variant.price().amount());
        entity.setPriceCurrency(variant.price().currencyCode());
        entity.setEnabled(variant.enabled());
        entity.setStockLevel(variant.stockLevel());
        return entity;
    }

    default void updateEntity(@MappingTarget ProductEntity entity, Product product) {
        entity.setName(product.name());
        entity.setSlug(product.slug());
        entity.setDescription(product.description());
        entity.setEnabled(product.enabled());
        entity.setFeaturedAssetId(product.featuredAssetId());
        entity.getAssetIds().clear();
        if (product.assetIds() != null) {
            entity.getAssetIds().addAll(product.assetIds());
        }
        entity.setSyncedAt(product.syncedAt());

        entity.clearVariants();
        for (ProductVariant variant : product.variants()) {
            ProductVariantEntity variantEntity = toVariantEntity(variant);
            entity.addVariant(variantEntity);
        }
    }
}
