package com.microservice.quarkus.catalog.tours.domain;

import java.util.List;
import java.util.Optional;

public interface TourRepository {

  void save(Tour tour);

  void update(Tour tour);

  Optional<Tour> findById(TourId id);

  Optional<Tour> findByCode(TourCode code);

  /**
   * Find all versions of a tour by code, ordered by version descending (newest first)
   */
  List<Tour> findAllVersionsByCode(TourCode code);

  /**
   * Find the latest version of a tour by code
   */
  Optional<Tour> findLatestVersionByCode(TourCode code);

  List<Tour> findAll();

  List<Tour> findByStatus(TourStatus status);

  List<Tour> findPublished();

  boolean existsByCode(TourCode code);

  void delete(TourId id);
}
