package com.microservice.quarkus.user.passenger.domain.shared;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
  UUID eventId();

  Instant occurredOn();
}
