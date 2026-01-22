package com.microservice.quarkus.catalog.tours.domain.events;

import com.microservice.quarkus.catalog.tours.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record TourDiscontinuedEvent(
    String tourId,
    String code,
    UUID eventId,
    Instant occurredOn) implements DomainEvent {

  public TourDiscontinuedEvent(String tourId, String code) {
    this(tourId, code, UUID.randomUUID(), Instant.now());
  }
}
