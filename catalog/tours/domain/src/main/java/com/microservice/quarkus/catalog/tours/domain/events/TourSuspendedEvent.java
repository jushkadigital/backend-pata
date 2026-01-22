package com.microservice.quarkus.catalog.tours.domain.events;

import com.microservice.quarkus.catalog.tours.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record TourSuspendedEvent(
    String tourId,
    String code,
    String reason,
    UUID eventId,
    Instant occurredOn) implements DomainEvent {

  public TourSuspendedEvent(String tourId, String code, String reason) {
    this(tourId, code, reason, UUID.randomUUID(), Instant.now());
  }
}
