package com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

public record UserDeletedEventDTO(
    @NotBlank String aggregateId,
    @NotBlank String aggregateType,
    @NotBlank String eventType,
    @Positive int eventVersion,
    @NotBlank String correlationId,
    String causationId,
    String traceId,
    String spanId,
    @NotBlank String producer,
    String actorId,
    String tenantId,
    @NotNull UUID eventId,
    @NotNull Instant occurredOn,
    String email,
    String userType) {
}
