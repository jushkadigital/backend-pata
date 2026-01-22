package com.microservice.quarkus.user.shared.domain.outbox;

public enum EventScope {
  INTERNAL_ONLY, // Solo EventBus (Vert.x)
  EXTERNAL_ONLY, // Solo RabbitMQ
  BOTH // Ambos (Pragmático, pero úsalo con cuidado)
}
