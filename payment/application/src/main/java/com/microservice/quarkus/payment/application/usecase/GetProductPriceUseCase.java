package com.microservice.quarkus.payment.application.usecase;

import com.microservice.quarkus.payment.application.dto.PriceQuery;
import com.microservice.quarkus.payment.domain.Money;
import com.microservice.quarkus.payment.domain.ProductId;
import com.microservice.quarkus.payment.domain.ProductRepository;
import com.microservice.quarkus.payment.domain.VariantId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

/**
 * Use case for retrieving product prices.
 */
@ApplicationScoped
public class GetProductPriceUseCase {

    private final ProductRepository productRepository;

    @Inject
    public GetProductPriceUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Gets the price for a product or variant.
     */
    public Optional<Money> execute(PriceQuery query) {
        return productRepository.findById(ProductId.of(query.productId()))
            .flatMap(product -> {
                if (query.hasVariant()) {
                    return product.variants().stream()
                        .filter(v -> v.id().equals(VariantId.of(query.variantId())))
                        .findFirst()
                        .map(v -> v.price());
                }
                return product.hasVariants()
                    ? Optional.of(product.defaultVariant().price())
                    : Optional.empty();
            });
    }

    /**
     * Gets the default price for a product.
     */
    public Optional<Money> getDefaultPrice(String productId) {
        return execute(new PriceQuery(productId));
    }

    /**
     * Calculates total price for quantity.
     */
    public Optional<Money> calculateTotal(String productId, int quantity) {
        return getDefaultPrice(productId)
            .map(price -> price.multiply(quantity));
    }

    /**
     * Calculates total price for a specific variant.
     */
    public Optional<Money> calculateVariantTotal(String productId, String variantId, int quantity) {
        return execute(new PriceQuery(productId, variantId, quantity))
            .map(price -> price.multiply(quantity));
    }
}
