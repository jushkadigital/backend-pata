package com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.catalog.tours.domain.Tour;
import com.microservice.quarkus.catalog.tours.domain.TourCode;
import com.microservice.quarkus.catalog.tours.domain.TourId;
import com.microservice.quarkus.catalog.tours.domain.TourRepository;
import com.microservice.quarkus.catalog.tours.domain.TourStatus;
import com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.dbo.TourEntity;
import com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.mapper.TourMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class TourDboRepository implements TourRepository {

    @Inject
    TourPanacheRepository repository;

    @Inject
    TourMapper tourMapper;

    @Override
    public Optional<Tour> findById(TourId id) {
        return repository.findByIdOptional(id.value())
                .map(tourMapper::toDomain);
    }

    @Override
    public Optional<Tour> findByCode(TourCode code) {
        return repository.findByCode(code.value())
                .map(tourMapper::toDomain);
    }

    @Override
    public List<Tour> findAllVersionsByCode(TourCode code) {
        return repository.findAllVersionsByCode(code.value()).stream()
                .map(tourMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Tour> findLatestVersionByCode(TourCode code) {
        return repository.findLatestVersionByCode(code.value())
                .map(tourMapper::toDomain);
    }

    @Override
    public List<Tour> findAll() {
        return repository.findAll().stream()
                .map(tourMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tour> findByStatus(TourStatus status) {
        return repository.findByStatus(status).stream()
                .map(tourMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tour> findPublished() {
        return repository.findPublished().stream()
                .map(tourMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void save(Tour tour) {
        TourEntity entity = tourMapper.toDbo(tour);
        repository.persistAndFlush(entity);
    }

    @Override
    @Transactional
    public void update(Tour tour) {
        TourEntity entity = repository.findByIdOptional(tour.getId().value())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No tour found for ID: " + tour.getId().value()));
        tourMapper.updateEntityFromDomain(tour, entity);
        repository.persist(entity);
    }

    @Override
    @Transactional
    public void delete(TourId id) {
        repository.deleteById(id.value());
    }

    @Override
    public boolean existsByCode(TourCode code) {
        return repository.existsByCode(code.value());
    }
}
