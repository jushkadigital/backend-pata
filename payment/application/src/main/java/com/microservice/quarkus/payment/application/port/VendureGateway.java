package com.microservice.quarkus.payment.application.port;

import com.microservice.quarkus.payment.domain.Product;
import com.microservice.quarkus.payment.domain.ProductId;
import com.microservice.quarkus.payment.domain.ProductVariant;
import com.microservice.quarkus.payment.domain.VariantId;

import java.util.List;
import java.util.Optional;

/**
 * Port for Vendure e-commerce platform integration.
 * Implemented by VendureACL in infrastructure layer.
 */
public interface VendureGateway {

    /**
     * Fetches all products from Vendure catalog.
     */
    List<Product> fetchAllProducts();

    /**
     * Fetches all products with pagination.
     */
    List<Product> fetchProducts(int skip, int take);

    /**
     * Fetches a product by its Vendure ID.
     */
    Optional<Product> fetchProductById(ProductId id);

    /**
     * Fetches a product by its slug.
     */
    Optional<Product> fetchProductBySlug(String slug);

    /**
     * Fetches a specific variant by ID.
     */
    Optional<ProductVariant> fetchVariantById(VariantId id);

    /**
     * Searches products by term.
     */
    List<Product> searchProducts(String term);

    /**
     * Checks if Vendure service is available.
     */
    boolean isAvailable();
}
