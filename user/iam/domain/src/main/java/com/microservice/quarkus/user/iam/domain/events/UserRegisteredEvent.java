package com.microservice.quarkus.user.iam.domain.events;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.UserType;

public record UserRegisteredEvent(String externalId, String email, String type, String eventId,
    Instant ocurredOn) {
  public UserRegisteredEvent(String externalId, EmailAddress email, UserType type) {
    this(externalId, email.value(), type.toString(), UUID.randomUUID().toString(), Instant.now());
  }

}
