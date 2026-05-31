package com.microservice.quarkus.user.identity.application.event;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.shared.domain.DomainEvent;

public record IdentityUserDeleted(
    UUID eventId,
    String externalId,
    Instant occurredOn) implements DomainEvent {

  public IdentityUserDeleted(String externalId) {
    this(UUID.randomUUID(), externalId, Instant.now());
  }

  @Override
  public String eventType() {
    return "identity.user.deleted";
  }
}
