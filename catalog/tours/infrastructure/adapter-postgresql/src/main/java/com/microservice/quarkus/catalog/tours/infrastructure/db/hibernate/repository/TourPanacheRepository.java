package com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.catalog.tours.domain.TourStatus;
import com.microservice.quarkus.catalog.tours.infrastructure.db.hibernate.dbo.TourEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TourPanacheRepository implements PanacheRepositoryBase<TourEntity, String> {

    public Optional<TourEntity> findByCode(String code) {
        return find("code", code).firstResultOptional();
    }

    /**
     * Find all versions of a tour by code, ordered by version descending
     */
    public List<TourEntity> findAllVersionsByCode(String code) {
        return list("code = ?1 order by version desc", code);
    }

    /**
     * Find the latest version of a tour by code
     */
    public Optional<TourEntity> findLatestVersionByCode(String code) {
        return find("code = ?1 order by version desc", code).firstResultOptional();
    }

    public List<TourEntity> findByStatus(TourStatus status) {
        return list("status", status);
    }

    public List<TourEntity> findPublished() {
        return list("status", TourStatus.PUBLISHED);
    }

    public boolean existsByCode(String code) {
        return count("code", code) > 0;
    }
}
