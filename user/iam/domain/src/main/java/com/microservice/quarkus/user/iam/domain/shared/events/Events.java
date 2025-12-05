package com.microservice.quarkus.user.iam.domain.shared.events;

import java.time.Instant;
import java.util.UUID;

public interface Events {
  UUID eventId();

  Instant occurredOn();
}
