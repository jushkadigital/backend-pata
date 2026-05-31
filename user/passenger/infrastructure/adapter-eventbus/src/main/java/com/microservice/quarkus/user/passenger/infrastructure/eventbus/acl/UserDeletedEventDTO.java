package com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl;

import java.time.Instant;
import java.util.UUID;

public record UserDeletedEventDTO(
    String aggregateId,
    String aggregateType,
    String eventType,
    int eventVersion,
    String correlationId,
    String causationId,
    String traceId,
    String spanId,
    String producer,
    String actorId,
    String tenantId,
    UUID eventId,
    Instant occurredOn) {
}
