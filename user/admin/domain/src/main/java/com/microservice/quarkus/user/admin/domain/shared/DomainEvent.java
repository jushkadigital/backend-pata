package com.microservice.quarkus.user.admin.domain.shared;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
  UUID eventId();

  Instant occurredOn();
}
