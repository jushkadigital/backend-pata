package com.microservice.quarkus.user.iam.domain.events;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.UserType;

public record UserRegisteredEvent(String externalId, String email, String type, String subType, String eventId,
    Instant ocurredOn) {
  public UserRegisteredEvent(String externalId, EmailAddress email, String type, String subType) {
    this(externalId, email.value(), type, subType, UUID.randomUUID().toString(), Instant.now());
  }

}
