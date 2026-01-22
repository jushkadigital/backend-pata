package com.microservice.quarkus.payment.infrastructure.postgresql;

import com.microservice.quarkus.payment.domain.Product;
import com.microservice.quarkus.payment.domain.ProductId;
import com.microservice.quarkus.payment.domain.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductPanacheRepository panacheRepository;
    private final ProductMapper mapper;

    @Inject
    public ProductRepositoryImpl(ProductPanacheRepository panacheRepository, ProductMapper mapper) {
        this.panacheRepository = panacheRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return panacheRepository.findByIdOptional(id.value())
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Product> findBySlug(String slug) {
        return panacheRepository.findBySlug(slug)
            .map(mapper::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return mapper.toDomainList(panacheRepository.listAll());
    }

    @Override
    public List<Product> findAllEnabled() {
        return mapper.toDomainList(panacheRepository.findAllEnabled());
    }

    @Override
    public void save(Product product) {
        panacheRepository.findByIdOptional(product.id().value())
            .ifPresentOrElse(
                existing -> mapper.updateEntity(existing, product),
                () -> panacheRepository.persist(mapper.toEntity(product))
            );
    }

    @Override
    public void saveAll(List<Product> products) {
        products.forEach(this::save);
    }

    @Override
    public boolean existsById(ProductId id) {
        return panacheRepository.findByIdOptional(id.value()).isPresent();
    }
}
