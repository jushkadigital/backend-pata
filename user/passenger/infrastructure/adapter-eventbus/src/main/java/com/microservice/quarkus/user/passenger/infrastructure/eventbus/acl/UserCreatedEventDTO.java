package com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserCreatedEventDTO(
    @NotBlank String aggregateId,
    @NotBlank String aggregateType,
    @NotBlank String eventType,
    @Positive int eventVersion,
    @NotBlank String email,
    @NotBlank String userType,
    List<String> clientRoles,
    @NotBlank String correlationId,
    String causationId,
    String traceId,
    String spanId,
    @NotBlank String producer,
    String actorId,
    String tenantId,
    @NotNull UUID eventId,
    @NotNull Instant occurredOn) {
}
