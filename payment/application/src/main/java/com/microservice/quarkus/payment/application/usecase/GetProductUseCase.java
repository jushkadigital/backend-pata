package com.microservice.quarkus.payment.application.usecase;

import com.microservice.quarkus.payment.domain.Product;
import com.microservice.quarkus.payment.domain.ProductId;
import com.microservice.quarkus.payment.domain.ProductRepository;
import com.microservice.quarkus.payment.domain.ProductVariant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

/**
 * Use case for querying products from local cache.
 */
@ApplicationScoped
public class GetProductUseCase {

    private final ProductRepository productRepository;

    @Inject
    public GetProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Gets a product by ID.
     */
    public Optional<Product> findById(String productId) {
        return productRepository.findById(ProductId.of(productId));
    }

    /**
     * Gets a product by slug.
     */
    public Optional<Product> findBySlug(String slug) {
        return productRepository.findBySlug(slug);
    }

    /**
     * Gets all products.
     */
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /**
     * Gets all enabled products.
     */
    public List<Product> findAllEnabled() {
        return productRepository.findAllEnabled();
    }

    /**
     * Checks if a product exists.
     */
    public boolean exists(String productId) {
        return productRepository.existsById(ProductId.of(productId));
    }

    /**
     * Finds a product variant by SKU.
     */
    public Optional<ProductVariant> findBySku(String sku) {
        return productRepository.findAll().stream()
                .flatMap(product -> product.variants().stream())
                .filter(variant -> variant.sku().equals(sku))
                .findFirst();
    }
}
