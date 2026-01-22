package com.microservice.quarkus.user.passenger.domain.events;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.passenger.domain.shared.DomainEvent;

public record PassengerRegisteredEvent(
    String externalId,
    String email,
    String passengerType,
    UUID eventId,
    Instant occurredOn) implements DomainEvent {

  public PassengerRegisteredEvent(String externalId, String email, String passengerType) {
    this(externalId, email, passengerType, UUID.randomUUID(), Instant.now());
  }
}
