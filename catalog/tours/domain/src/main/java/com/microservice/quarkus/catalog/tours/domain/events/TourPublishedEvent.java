package com.microservice.quarkus.catalog.tours.domain.events;

import com.microservice.quarkus.catalog.tours.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event emitted when a tour is published.
 * Contains all data needed for payment module to create the product in Vendure.
 */
public record TourPublishedEvent(
    String tourId,
    String code,
    String name,
    String description,
    List<CombinationData> combinations,
    UUID eventId,
    Instant occurredOn) implements DomainEvent {

  public TourPublishedEvent(String tourId, String code, String name, String description,
                            List<CombinationData> combinations) {
    this(tourId, code, name, description, combinations, UUID.randomUUID(), Instant.now());
  }

  /**
   * Data about a service combination for Vendure product variant creation.
   */
  public record CombinationData(
      String sku,
      String name,
      String description
  ) {}
}
