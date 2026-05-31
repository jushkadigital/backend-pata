package com.microservice.quarkus.user.admin.infrastructure.eventbus.acl;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEventDTO(
    String aggregateId,
    String aggregateType,
    String eventType,
    int eventVersion,
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
    UUID eventId,
    Instant occurredOn) {
}
