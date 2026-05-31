package com.microservice.quarkus.user.passenger.application.event;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.shared.application.IntegrationEvent;

public record PassengerRegisteredEvent(
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
    int eventVersion,
    UUID eventId,
    Instant occurredOn) implements IntegrationEvent {

  public static final int CURRENT_VERSION = 1;

  public PassengerRegisteredEvent(String aggregateId, String email, String passengerType,
                                    String correlationId, String causationId, String traceId, String spanId,
                                    String producer, String actorId, String tenantId) {
    this(aggregateId, email, passengerType, correlationId, causationId, traceId, spanId,
        producer, actorId, tenantId, CURRENT_VERSION, UUID.randomUUID(), Instant.now());
  }

  @Override
  public String eventType() {
    return "passenger.registered.v1";
  }

  @Override
  public String aggregateType() {
    return "Passenger";
  }
}
