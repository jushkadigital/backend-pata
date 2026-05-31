package com.microservice.quarkus.user.identity.application.event;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.shared.domain.DomainEvent;

public record IdentityUserRegistered(
    UUID eventId,
    String externalId,
    String email,
    String userType,
    String userSubType,
    Instant occurredOn) implements DomainEvent {

  public IdentityUserRegistered(String externalId, String email, String userType, String userSubType) {
    this(UUID.randomUUID(), externalId, email, userType, userSubType, Instant.now());
  }

  @Override
  public String eventType() {
    return "identity.user.registered";
  }
}
