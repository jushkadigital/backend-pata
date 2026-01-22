package com.microservice.quarkus.payment.domain;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Product aggregate.
 * Infrastructure layer provides the implementation.
 */
public interface ProductRepository {

    Optional<Product> findById(ProductId id);

    Optional<Product> findBySlug(String slug);

    List<Product> findAll();

    List<Product> findAllEnabled();

    void save(Product product);

    void saveAll(List<Product> products);

    boolean existsById(ProductId id);
}
