package com.microservice.quarkus.user.identity.application.service;

import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.dto.UserType;
import com.microservice.quarkus.user.identity.application.event.IdentityUserDeleted;
import com.microservice.quarkus.user.identity.application.event.IdentityUserRegistered;
import com.microservice.quarkus.user.identity.application.event.UserDeletedEvent;
import com.microservice.quarkus.user.identity.application.event.UserRegisteredEvent;
import com.microservice.quarkus.user.shared.application.NotificationEvent;
import com.microservice.quarkus.user.shared.application.outbox.EventMetadata;
import com.microservice.quarkus.user.shared.application.outbox.EventScope;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEventRepository;
import com.microservice.quarkus.user.shared.application.outbox.TraceContextProvider;
import com.microservice.quarkus.user.shared.application.saga.SagaInstance;
import com.microservice.quarkus.user.shared.application.saga.SagaMetrics;
import com.microservice.quarkus.user.shared.application.saga.SagaRepository;

import io.vertx.core.json.JsonObject;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IdentityPersistenceService {

  private static final String PRODUCER = "user-service";

  private final IdentitySyncRepository syncRepository;
  private final OutboxEventRepository outboxEventRepository;
  private final SagaRepository sagaRepository;
  private final SagaMetrics sagaMetrics;
  private final TraceContextProvider traceContextProvider;
  private final IdentityMetrics identityMetrics;

  public IdentityPersistenceService(IdentitySyncRepository syncRepository,
                                     OutboxEventRepository outboxEventRepository,
                                     SagaRepository sagaRepository,
                                     SagaMetrics sagaMetrics,
                                     TraceContextProvider traceContextProvider,
                                     IdentityMetrics identityMetrics) {
    this.syncRepository = syncRepository;
    this.outboxEventRepository = outboxEventRepository;
    this.sagaRepository = sagaRepository;
    this.sagaMetrics = sagaMetrics;
    this.traceContextProvider = traceContextProvider;
    this.identityMetrics = identityMetrics;
  }

  @Transactional
  public void persistFailedSync(String email, UserType type) {
    UserSyncRecord record = UserSyncRecord.createNew(email, type)
        .withSyncStatus(SyncStatus.FAILED);
    syncRepository.save(record);
  }

  @WithSpan("identity.user.persistPending")
  @Transactional
  public void persistPendingSync(String email, UserType type, String externalId,
                                  String correlationId, String causationId,
                                  EventMetadata traceMeta, String actorId) {
    UserSyncRecord record = UserSyncRecord.createNew(email, type)
        .withExternalId(externalId)
        .withSyncStatus(SyncStatus.PENDING);
    syncRepository.save(record);
  }

  @WithSpan("identity.user.completeSync")
  @Transactional
  public void completeSync(UserSyncRecord record) {
    UserSyncRecord synced = record.withSyncStatus(SyncStatus.SYNCED);
    syncRepository.save(synced);

    SagaInstance saga = SagaInstance.start("UserRegistrationSaga",
        record.id(), record.externalId(), "user-synced");
    sagaRepository.save(saga);
    sagaMetrics.recordStarted();

    IdentityUserRegistered domainEvent = new IdentityUserRegistered(
        record.externalId(), record.email(), record.type().name(), null);

    publishIntegrationEvent(domainEvent, record.id(), null, traceContextProvider.current(), null);
    publishNotificationEvent(domainEvent, record.id(), null, traceContextProvider.current(), null);
    identityMetrics.recordRegistered();
  }

  @WithSpan("identity.user.persist")
  @Transactional
  public void persistSyncAndOutbox(String email, UserType type, String externalId,
                                    IdentityUserRegistered domainEvent,
                                    String correlationId, String causationId,
                                    EventMetadata traceMeta, String actorId) {
    UserSyncRecord record = UserSyncRecord.createNew(email, type)
        .withExternalId(externalId)
        .withSyncStatus(SyncStatus.SYNCED);
    syncRepository.save(record);

    SagaInstance saga = SagaInstance.start(
        "UserRegistrationSaga", correlationId, externalId, "user-synced");
    sagaRepository.save(saga);
    sagaMetrics.recordStarted();

    publishIntegrationEvent(domainEvent, correlationId, causationId, traceMeta, actorId);
    publishNotificationEvent(domainEvent, correlationId, causationId, traceMeta, actorId);
    identityMetrics.recordRegistered();
  }

  private void publishIntegrationEvent(IdentityUserRegistered domainEvent,
                                       String correlationId, String causationId,
                                       EventMetadata traceMeta, String actorId) {
    UserRegisteredEvent event = new UserRegisteredEvent(
        domainEvent.externalId(), domainEvent.email(), domainEvent.userType(), domainEvent.userSubType(),
        correlationId, causationId, traceMeta.traceId(), traceMeta.spanId(),
        PRODUCER, actorId, null);

    OutboxEvent outboxEvent = OutboxEvent.create(
        event.eventType(),
        event.eventVersion(),
        event.aggregateType(),
        domainEvent.externalId(),
        correlationId,
        causationId,
        traceMeta.traceId(),
        traceMeta.spanId(),
        PRODUCER,
        actorId,
        null,
        JsonObject.mapFrom(event).encode(),
        EventScope.BOTH,
        event.occurredOn());
    outboxEventRepository.save(outboxEvent);
  }

  private void publishNotificationEvent(IdentityUserRegistered domainEvent,
                                        String correlationId, String causationId,
                                        EventMetadata traceMeta, String actorId) {
    UserRegisteredEvent integrationEvent = new UserRegisteredEvent(
        domainEvent.externalId(), domainEvent.email(), domainEvent.userType(), domainEvent.userSubType(),
        correlationId, causationId, traceMeta.traceId(), traceMeta.spanId(),
        PRODUCER, actorId, null);

    NotificationEvent notification = NotificationEvent.from(integrationEvent, domainEvent.externalId(), integrationEvent.aggregateType());
    OutboxEvent notificationOutbox = OutboxEvent.create(
        "notification.identity.user.registered.v1",
        notification.eventVersion(),
        notification.aggregateType(),
        domainEvent.externalId(),
        correlationId,
        causationId,
        traceMeta.traceId(),
        traceMeta.spanId(),
        PRODUCER,
        actorId,
        null,
        JsonObject.mapFrom(notification).encode(),
        EventScope.EXTERNAL_ONLY,
        domainEvent.occurredOn());
    outboxEventRepository.save(notificationOutbox);
  }

  @WithSpan("identity.user.delete-persist")
  @Transactional
  public void deleteSyncRecord(String externalId, String correlationId) {
    syncRepository.deleteByExternalId(externalId);

    EventMetadata traceMeta = traceContextProvider.current();
    UserDeletedEvent event = new UserDeletedEvent(
        externalId, correlationId, null, traceMeta.traceId(), traceMeta.spanId(),
        PRODUCER, null, null);

    OutboxEvent outboxEvent = OutboxEvent.create(
        event.eventType(),
        event.eventVersion(),
        event.aggregateType(),
        externalId,
        correlationId,
        null,
        traceMeta.traceId(),
        traceMeta.spanId(),
        PRODUCER,
        null,
        null,
        JsonObject.mapFrom(event).encode(),
        EventScope.BOTH,
        event.occurredOn());
    outboxEventRepository.save(outboxEvent);
    identityMetrics.recordDeleted();
  }
}
