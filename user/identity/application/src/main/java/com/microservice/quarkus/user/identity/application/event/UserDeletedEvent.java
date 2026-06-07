package com.microservice.quarkus.user.identity.application.event;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.shared.application.IntegrationEvent;

public record UserDeletedEvent(
    String eventType,
    String aggregateType,
    String aggregateId,
    String correlationId,
    String causationId,
    String traceId,
    String spanId,
    String producer,
    String actorId,
    String tenantId,
    String specVersion,
    int eventVersion,
    UUID eventId,
    Instant occurredOn,
    String email,
    String userType) implements IntegrationEvent {

  public static final int CURRENT_VERSION = 1;
  public static final String CURRENT_SPEC_VERSION = "tourism-events/v1";
  public static final String EVENT_TYPE = "identity.user.deleted.v1";
  public static final String AGGREGATE_TYPE = "User";

  public UserDeletedEvent(String aggregateId,
                          String correlationId, String causationId,
                          String traceId, String spanId,
                          String producer, String actorId, String tenantId,
                          String email, String userType) {
    this(EVENT_TYPE, AGGREGATE_TYPE, aggregateId,
        correlationId, causationId, traceId, spanId,
        producer, actorId, tenantId, CURRENT_SPEC_VERSION, CURRENT_VERSION, UUID.randomUUID(), Instant.now(),
        email, userType);
  }

  public static UserDeletedEvent v1(String aggregateId,
                                      String correlationId, String causationId,
                                      String traceId, String spanId,
                                      String producer, String actorId, String tenantId,
                                      String email, String userType) {
    return new UserDeletedEvent(aggregateId, correlationId, causationId,
        traceId, spanId, producer, actorId, tenantId, email, userType);
  }
}
