package com.microservice.quarkus.user.identity.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.microservice.quarkus.user.shared.domain.DomainEvent;

public record IdentityUserCreated(
    UUID eventId,
    String externalId,
    String email,
    String userType,      // NEW
    List<String> roles,   // NOW: composite roles only
    Instant occurredOn) implements DomainEvent {

  public IdentityUserCreated(String externalId, String email, String userType, List<String> roles) {
    this(UUID.randomUUID(), externalId, email, userType, roles, Instant.now());
  }

  @Override
  public String eventType() {
    return "identity.user.created";
  }
}
