package com.microservice.quarkus.user.identity.application.event;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.shared.domain.DomainEvent;

public record IdentityUserAuthenticated(
    UUID eventId,
    String externalId,
    String email,
    String authMethod,
    Instant occurredOn) implements DomainEvent {

  public IdentityUserAuthenticated(String externalId, String email, String authMethod) {
    this(UUID.randomUUID(), externalId, email, authMethod, Instant.now());
  }

  @Override
  public String eventType() {
    return "identity.user.authenticated";
  }
}
