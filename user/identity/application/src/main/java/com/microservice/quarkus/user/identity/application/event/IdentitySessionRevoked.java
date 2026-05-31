package com.microservice.quarkus.user.identity.application.event;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.shared.domain.DomainEvent;

public record IdentitySessionRevoked(
    UUID eventId,
    String externalId,
    String sessionId,
    String reason,
    Instant occurredOn) implements DomainEvent {

  public IdentitySessionRevoked(String externalId, String sessionId, String reason) {
    this(UUID.randomUUID(), externalId, sessionId, reason, Instant.now());
  }

  @Override
  public String eventType() {
    return "identity.session.revoked";
  }
}
