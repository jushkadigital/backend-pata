package com.microservice.quarkus.catalog.tours.domain.events;

import com.microservice.quarkus.catalog.tours.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a tour is created.
 * Contains basic tour data for downstream consumers.
 */
public record TourCreatedEvent(
    String tourId,
    String code,
    String name,
    String description,
    String duration,
    int includedServicesCount,
    UUID eventId,
    Instant occurredOn) implements DomainEvent {

  public TourCreatedEvent(String tourId, String code, String name, String description,
                          String duration, int includedServicesCount) {
    this(tourId, code, name, description, duration, includedServicesCount,
         UUID.randomUUID(), Instant.now());
  }
}
