package com.microservice.quarkus.catalog.tours.domain.events;

import com.microservice.quarkus.catalog.tours.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event emitted when a tour is updated.
 * Contains updated tour data including combinations for Vendure sync.
 */
public record TourUpdatedEvent(
    String tourId,
    String code,
    String name,
    String description,
    String duration,
    List<CombinationData> combinations,
    UUID eventId,
    Instant occurredOn) implements DomainEvent {

  public TourUpdatedEvent(String tourId, String code, String name, String description,
                          String duration, List<CombinationData> combinations) {
    this(tourId, code, name, description, duration, combinations,
         UUID.randomUUID(), Instant.now());
  }

  /**
   * Data about a service combination for Vendure product variant.
   */
  public record CombinationData(
      String sku,
      String name,
      String description
  ) {}
}
