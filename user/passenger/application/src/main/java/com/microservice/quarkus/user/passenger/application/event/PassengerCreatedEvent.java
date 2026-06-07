package com.microservice.quarkus.user.passenger.application.event;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.shared.application.IntegrationEvent;

public record PassengerCreatedEvent(
    String aggregateId,
    String email,
    String passengerType,
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

  public PassengerCreatedEvent(String aggregateId, String email, String passengerType,
                                     String correlationId, String causationId, String traceId, String spanId,
                                     String producer, String actorId, String tenantId) {
    this(aggregateId, email, passengerType, correlationId, causationId, traceId, spanId,
        producer, actorId, tenantId, CURRENT_SPEC_VERSION, CURRENT_VERSION, UUID.randomUUID(), Instant.now());
  }

  public static PassengerCreatedEvent v1(String aggregateId, String email, String passengerType,
                                       String correlationId, String causationId, String traceId, String spanId,
                                       String producer, String actorId, String tenantId) {
    return new PassengerCreatedEvent(aggregateId, email, passengerType, correlationId, causationId,
        traceId, spanId, producer, actorId, tenantId);
  }

  @Override
  public String eventType() {
    return "passenger.created.v1";
  }

  @Override
  public String aggregateType() {
    return "Passenger";
  }
}
