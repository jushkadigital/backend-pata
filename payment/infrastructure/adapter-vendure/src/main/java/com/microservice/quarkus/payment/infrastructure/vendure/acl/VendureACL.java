package com.microservice.quarkus.payment.infrastructure.vendure.acl;

import com.microservice.quarkus.payment.application.port.VendureGateway;
import com.microservice.quarkus.payment.domain.Product;
import com.microservice.quarkus.payment.domain.ProductId;
import com.microservice.quarkus.payment.domain.ProductVariant;
import com.microservice.quarkus.payment.domain.VariantId;
import com.microservice.quarkus.payment.infrastructure.vendure.acl.mapper.VendureProductMapper;
import com.microservice.quarkus.payment.infrastructure.vendure.acl.mapper.VendureVariantMapper;
import com.microservice.quarkus.payment.infrastructure.vendure.client.VendureGraphQLClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

/**
 * Anti-Corruption Layer for Vendure integration.
 * Translates between Vendure's model and our domain model.
 * Implements the VendureGateway port from application layer.
 */
@ApplicationScoped
public class VendureACL implements VendureGateway {

    private final VendureGraphQLClient graphQLClient;
    private final VendureProductMapper productMapper;
    private final VendureVariantMapper variantMapper;

    @Inject
    public VendureACL(
        VendureGraphQLClient graphQLClient,
        VendureProductMapper productMapper,
        VendureVariantMapper variantMapper
    ) {
        this.graphQLClient = graphQLClient;
        this.productMapper = productMapper;
        this.variantMapper = variantMapper;
    }

    @Override
    public List<Product> fetchAllProducts() {
        return productMapper.toDomainList(graphQLClient.fetchAllProducts());
    }

    @Override
    public List<Product> fetchProducts(int skip, int take) {
        return productMapper.toDomainList(graphQLClient.fetchProducts(skip, take));
    }

    @Override
    public Optional<Product> fetchProductById(ProductId id) {
        return graphQLClient.fetchProductById(id.value())
            .map(productMapper::toDomain);
    }

    @Override
    public Optional<Product> fetchProductBySlug(String slug) {
        return graphQLClient.fetchProductBySlug(slug)
            .map(productMapper::toDomain);
    }

    @Override
    public Optional<ProductVariant> fetchVariantById(VariantId id) {
        // Vendure shop API doesn't expose direct variant query
        return Optional.empty();
    }

    @Override
    public List<Product> searchProducts(String term) {
        // Search requires additional implementation - for now filter from all products
        String searchTerm = term.toLowerCase();
        return fetchAllProducts().stream()
                .filter(p -> p.name().toLowerCase().contains(searchTerm) ||
                        p.slug().toLowerCase().contains(searchTerm))
                .toList();
    }

    @Override
    public boolean isAvailable() {
        return graphQLClient.isAvailable();
    }
}
