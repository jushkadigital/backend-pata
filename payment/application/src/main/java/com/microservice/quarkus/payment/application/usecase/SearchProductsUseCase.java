package com.microservice.quarkus.payment.application.usecase;

import com.microservice.quarkus.payment.application.port.VendureGateway;
import com.microservice.quarkus.payment.domain.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for searching products directly in Vendure.
 * Useful for real-time search without requiring local sync.
 */
@ApplicationScoped
public class SearchProductsUseCase {

    private final VendureGateway vendureGateway;

    @Inject
    public SearchProductsUseCase(VendureGateway vendureGateway) {
        this.vendureGateway = vendureGateway;
    }

    /**
     * Searches products in Vendure by term.
     */
    public List<Product> execute(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return List.of();
        }
        return vendureGateway.searchProducts(searchTerm.trim());
    }

    /**
     * Alias for execute - searches products in Vendure by term.
     */
    public List<Product> search(String searchTerm) {
        return execute(searchTerm);
    }
}
