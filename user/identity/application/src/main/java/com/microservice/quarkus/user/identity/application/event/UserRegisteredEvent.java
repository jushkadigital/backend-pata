package com.microservice.quarkus.user.identity.application.event;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.shared.application.IntegrationEvent;

public record UserRegisteredEvent(
    String eventType,
    String aggregateType,
    String aggregateId,
    String email,
    String type,
    String subType,
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
  public static final String EVENT_TYPE = "identity.user.registered.v1";
  public static final String AGGREGATE_TYPE = "User";

  public UserRegisteredEvent(String aggregateId, String email, String type, String subType,
                              String correlationId, String causationId, String traceId, String spanId,
                              String producer, String actorId, String tenantId) {
    this(EVENT_TYPE, AGGREGATE_TYPE, aggregateId, email, type, subType,
        correlationId, causationId, traceId, spanId,
        producer, actorId, tenantId, CURRENT_VERSION, UUID.randomUUID(), Instant.now());
  }
}
