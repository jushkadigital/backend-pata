package com.microservice.quarkus.user.admin.domain.events;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.admin.domain.shared.DomainEvent;

public record AdminRegisteredEvent(
    String externalId,
    String email,
    String adminType,
    UUID eventId,
    Instant occurredOn) implements DomainEvent {

  public AdminRegisteredEvent(String externalId, String email, String adminType) {
    this(externalId, email, adminType, UUID.randomUUID(), Instant.now());
  }
}
