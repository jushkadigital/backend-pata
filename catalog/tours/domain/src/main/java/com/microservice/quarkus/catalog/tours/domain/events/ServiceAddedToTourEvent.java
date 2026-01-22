package com.microservice.quarkus.catalog.tours.domain.events;

import com.microservice.quarkus.catalog.tours.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ServiceAddedToTourEvent(
    String tourId,
    String serviceName,
    String serviceType,
    boolean isMandatory,
    UUID eventId,
    Instant occurredOn) implements DomainEvent {

    public ServiceAddedToTourEvent(String tourId, String serviceName, String serviceType, boolean isMandatory) {
        this(tourId, serviceName, serviceType, isMandatory, UUID.randomUUID(), Instant.now());
    }
}
