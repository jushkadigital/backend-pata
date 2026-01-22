package com.microservice.quarkus.payment.infrastructure.postgresql;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProductPanacheRepository implements PanacheRepositoryBase<ProductEntity, String> {

    public Optional<ProductEntity> findBySlug(String slug) {
        return find("slug", slug).firstResultOptional();
    }

    public List<ProductEntity> findAllEnabled() {
        return list("enabled", true);
    }
}
