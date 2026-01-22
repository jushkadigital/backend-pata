package com.microservice.quarkus.payment.infrastructure.rest.mapper;

import com.microservice.quarkus.payment.domain.Money;
import com.microservice.quarkus.payment.domain.Product;
import com.microservice.quarkus.payment.domain.ProductVariant;
import com.microservice.quarkus.payment.infrastructure.rest.dto.MoneyDTO;
import com.microservice.quarkus.payment.infrastructure.rest.dto.ProductDTO;
import com.microservice.quarkus.payment.infrastructure.rest.dto.ProductVariantDTO;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi")
public interface PaymentRestMapper {

    default ProductDTO toDTO(Product product) {
        if (product == null) {
            return null;
        }

        ProductDTO dto = new ProductDTO();
        dto.setId(product.id().value());
        dto.setName(product.name());
        dto.setSlug(product.slug());
        dto.setDescription(product.description());
        dto.setEnabled(product.enabled());
        dto.setFeaturedAssetId(product.featuredAssetId());
        dto.setAssetIds(product.assetIds());
        dto.setSyncedAt(toDate(product.syncedAt()));

        if (product.variants() != null) {
            List<ProductVariantDTO> variantDTOs = product.variants().stream()
                    .map(this::toVariantDTO)
                    .collect(Collectors.toList());
            dto.setVariants(variantDTOs);
        }

        return dto;
    }

    default ProductVariantDTO toVariantDTO(ProductVariant variant) {
        if (variant == null) {
            return null;
        }

        ProductVariantDTO dto = new ProductVariantDTO();
        dto.setId(variant.id().value());
        dto.setSku(variant.sku());
        dto.setName(variant.name());
        dto.setPrice(toMoneyDTO(variant.price()));
        dto.setEnabled(variant.enabled());
        dto.setStockLevel(variant.stockLevel());

        return dto;
    }

    default MoneyDTO toMoneyDTO(Money money) {
        if (money == null) {
            return null;
        }

        MoneyDTO dto = new MoneyDTO();
        dto.setAmount(money.amount().doubleValue());
        dto.setCurrency(money.currencyCode());

        return dto;
    }

    default List<ProductDTO> toDTOList(List<Product> products) {
        return products.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    default Date toDate(Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }
}
