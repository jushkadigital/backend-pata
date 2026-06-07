package com.microservice.quarkus.user.shared.application;

import java.time.Instant;
import java.util.UUID;

public interface IntegrationEvent {
    UUID eventId();
    String eventType();
    int eventVersion();
    String aggregateId();
    String aggregateType();
    String correlationId();
    String causationId();
    String traceId();
    String spanId();
    String producer();
    String actorId();
    String tenantId();
    Instant occurredOn();
    String specVersion();
}
