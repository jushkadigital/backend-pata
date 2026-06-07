package com.microservice.quarkus.user.identity.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.microservice.quarkus.user.shared.application.IntegrationEvent;

public record UserCreatedEvent(
    String eventType,
    String aggregateType,
    String aggregateId,
    String email,
    String userType,        // NEW: PASSENGER or ADMIN
    List<String> clientRoles, // RENAMED from roles — now only Keycloak composite roles
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
    Instant occurredOn) implements IntegrationEvent {

  public static final int CURRENT_VERSION = 1;
  public static final String CURRENT_SPEC_VERSION = "tourism-events/v1";
  public static final String EVENT_TYPE = "identity.user.created.v1";
  public static final String AGGREGATE_TYPE = "User";

  public UserCreatedEvent(String aggregateId, String email, String userType, List<String> clientRoles,
                          String correlationId, String causationId, String traceId, String spanId,
                          String producer, String actorId, String tenantId) {
    this(EVENT_TYPE, AGGREGATE_TYPE, aggregateId, email, userType, clientRoles,
        correlationId, causationId, traceId, spanId,
        producer, actorId, tenantId, CURRENT_SPEC_VERSION, CURRENT_VERSION, UUID.randomUUID(), Instant.now());
  }

  public static UserCreatedEvent v1(String aggregateId, String email, String userType, List<String> clientRoles,
                                    String correlationId, String causationId, String traceId, String spanId,
                                    String producer, String actorId, String tenantId) {
    return new UserCreatedEvent(aggregateId, email, userType, clientRoles, correlationId, causationId,
        traceId, spanId, producer, actorId, tenantId);
  }
}
