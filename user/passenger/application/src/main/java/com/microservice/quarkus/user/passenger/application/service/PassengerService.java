package com.microservice.quarkus.user.passenger.application.service;

import com.microservice.quarkus.user.passenger.application.api.PassengerRepository;
import com.microservice.quarkus.user.passenger.application.dto.CompletePassengerCommand;
import com.microservice.quarkus.user.passenger.application.dto.CreatePassengerCommand;
import com.microservice.quarkus.user.passenger.application.event.PassengerCreatedEvent;
import com.microservice.quarkus.user.passenger.domain.Passenger;
import com.microservice.quarkus.user.shared.application.NotificationEvent;
import com.microservice.quarkus.user.shared.application.outbox.EventMetadata;
import com.microservice.quarkus.user.shared.application.outbox.EventScope;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEventRepository;
import com.microservice.quarkus.user.shared.application.outbox.TraceContextProvider;

import io.vertx.core.json.JsonObject;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PassengerService {

  private static final String PRODUCER = "user-service";

  private final PassengerRepository repository;
  private final OutboxEventRepository outboxEventRepository;
  private final TraceContextProvider traceContextProvider;
  private final PassengerMetrics passengerMetrics;

  public PassengerService(PassengerRepository repository,
                          OutboxEventRepository outboxEventRepository,
                          TraceContextProvider traceContextProvider,
                          PassengerMetrics passengerMetrics) {
    this.repository = repository;
    this.outboxEventRepository = outboxEventRepository;
    this.traceContextProvider = traceContextProvider;
    this.passengerMetrics = passengerMetrics;
  }

  @Transactional
  public String register(CreatePassengerCommand cmd) {
    return register(cmd, null, null, null);
  }

  @Transactional
  public String register(CreatePassengerCommand cmd, String correlationId) {
    return register(cmd, correlationId, null, null);
  }

  @WithSpan("passenger.create")
  @Transactional
  public String register(CreatePassengerCommand cmd, String correlationId, String causationId, String actorId) {
    Passenger passenger = Passenger.createNew(cmd.email(), cmd.externalId(), cmd.passengerType());
    repository.save(passenger);

    String effectiveCorrelationId = correlationId != null ? correlationId : java.util.UUID.randomUUID().toString();
    EventMetadata traceMeta = traceContextProvider.current();

    PassengerCreatedEvent event = PassengerCreatedEvent.v1(
        cmd.externalId(), cmd.email(), cmd.passengerType(),
        effectiveCorrelationId, causationId,
        traceMeta.traceId(), traceMeta.spanId(),
        PRODUCER, actorId, null);
    OutboxEvent outboxEvent = OutboxEvent.create(
        event.eventType(),
        event.eventVersion(),
        event.aggregateType(),
        passenger.getId().value(),
        effectiveCorrelationId,
        causationId,
        traceMeta.traceId(),
        traceMeta.spanId(),
        PRODUCER,
        actorId,
        null,
        PassengerCreatedEvent.CURRENT_SPEC_VERSION,
        JsonObject.mapFrom(event).encode(),
        EventScope.EXTERNAL_ONLY,
        event.occurredOn());
    outboxEventRepository.save(outboxEvent);

    NotificationEvent notification = NotificationEvent.from(event, passenger.getId().value(), event.aggregateType());
    OutboxEvent notificationOutbox = OutboxEvent.create(
        "notification.passenger.created.v1",
        notification.eventVersion(),
        notification.aggregateType(),
        passenger.getId().value(),
        effectiveCorrelationId,
        causationId,
        traceMeta.traceId(),
        traceMeta.spanId(),
        PRODUCER,
        actorId,
        null,
        PassengerCreatedEvent.CURRENT_SPEC_VERSION,
        JsonObject.mapFrom(notification).encode(),
        EventScope.EXTERNAL_ONLY,
        event.occurredOn());
    outboxEventRepository.save(notificationOutbox);
    passengerMetrics.recordCreated();

    return passenger.getId().value();
  }

  @WithSpan("passenger.complete")
  @Transactional
  public String complete(String externalId, CompletePassengerCommand cmd) {
    Passenger passenger = repository.findByExternalId(externalId)
        .orElseThrow(() -> new IllegalArgumentException("Passenger not found with externalId: " + externalId));

    passenger.completeProfile(cmd.firstNames(), cmd.lastNames(), cmd.dni(), cmd.phone());
    repository.save(passenger);

    return passenger.getId().value();
  }

  @WithSpan("passenger.delete")
  @Transactional
  public void deleteByExternalId(String externalId) {
    repository.deleteByExternalId(externalId);
    passengerMetrics.recordDeleted();
  }

}
