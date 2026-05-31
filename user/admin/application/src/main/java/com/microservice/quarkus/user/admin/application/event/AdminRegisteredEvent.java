package com.microservice.quarkus.user.admin.application.event;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.shared.application.IntegrationEvent;

public record AdminRegisteredEvent(
    String aggregateId,
    String email,
    String adminType,
    String correlationId,
    String causationId,
    String traceId,
    String spanId,
    String producer,
    String actorId,
    String tenantId,
    int eventVersion,
    UUID eventId,
    Instant occurredOn) implements IntegrationEvent {

  public static final int CURRENT_VERSION = 1;

  public AdminRegisteredEvent(String aggregateId, String email, String adminType,
                               String correlationId, String causationId, String traceId, String spanId,
                               String producer, String actorId, String tenantId) {
    this(aggregateId, email, adminType, correlationId, causationId, traceId, spanId,
        producer, actorId, tenantId, CURRENT_VERSION, UUID.randomUUID(), Instant.now());
  }

  @Override
  public String eventType() {
    return "admin.registered.v1";
  }

  @Override
  public String aggregateType() {
    return "Admin";
  }
}
