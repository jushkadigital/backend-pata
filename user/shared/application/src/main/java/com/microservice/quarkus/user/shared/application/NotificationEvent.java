package com.microservice.quarkus.user.shared.application;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(
    String eventType,
    int eventVersion,
    String correlationId,
    String causationId,
    String traceId,
    String spanId,
    String producer,
    String actorId,
    String tenantId,
    String aggregateId,
    String aggregateType,
    UUID eventId,
    Instant occurredOn) {

  public static NotificationEvent from(IntegrationEvent source, String aggregateId, String aggregateType) {
    return new NotificationEvent(
        source.eventType(),
        source.eventVersion(),
        source.correlationId(),
        source.causationId(),
        source.traceId(),
        source.spanId(),
        source.producer(),
        source.actorId(),
        source.tenantId(),
        aggregateId,
        aggregateType,
        source.eventId(),
        source.occurredOn());
  }
}
