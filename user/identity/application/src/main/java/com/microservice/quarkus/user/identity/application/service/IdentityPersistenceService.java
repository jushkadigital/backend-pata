package com.microservice.quarkus.user.identity.application.service;

import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.event.IdentityUserCreated;
import com.microservice.quarkus.user.identity.application.event.IdentityUserDeleted;
import com.microservice.quarkus.user.identity.application.event.UserCreatedEvent;
import com.microservice.quarkus.user.identity.application.event.UserDeletedEvent;
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

import java.util.List;

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
  public void persistFailedSync(String email, String userType, List<String> roles) {
    UserSyncRecord record = UserSyncRecord.createNew(email, userType, roles)
        .withSyncStatus(SyncStatus.FAILED);
    syncRepository.save(record);
  }

  @WithSpan("identity.user.persistPending")
  @Transactional
  public void persistPendingSync(String email, String userType, List<String> roles, String externalId,
                                    String correlationId, String causationId,
                                    EventMetadata traceMeta, String actorId) {
    UserSyncRecord record = UserSyncRecord.createNew(email, userType, roles)
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

    IdentityUserCreated domainEvent = new IdentityUserCreated(
        record.externalId(), record.email(), record.userType(), record.roles());

    publishIntegrationEvent(domainEvent, record.id(), null, traceContextProvider.current(), null);
    publishNotificationEvent(domainEvent, record.id(), null, traceContextProvider.current(), null);
    identityMetrics.recordRegistered();
  }

  @WithSpan("identity.user.persist")
  @Transactional
  public void persistSyncAndOutbox(String email, String userType, List<String> roles, String externalId,
                                     IdentityUserCreated domainEvent,
                                     String correlationId, String causationId,
                                     EventMetadata traceMeta, String actorId) {
    UserSyncRecord record = UserSyncRecord.createNew(email, userType, roles)
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

  private void publishIntegrationEvent(IdentityUserCreated domainEvent,
                                       String correlationId, String causationId,
                                       EventMetadata traceMeta, String actorId) {
    UserCreatedEvent event = UserCreatedEvent.v1(
        domainEvent.externalId(), domainEvent.email(), domainEvent.userType(), domainEvent.roles(),
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
        event.specVersion(),
        JsonObject.mapFrom(event).encode(),
        EventScope.BOTH,
        event.occurredOn());
    outboxEventRepository.save(outboxEvent);
  }

  private void publishNotificationEvent(IdentityUserCreated domainEvent,
                                          String correlationId, String causationId,
                                          EventMetadata traceMeta, String actorId) {
    UserCreatedEvent integrationEvent = UserCreatedEvent.v1(
        domainEvent.externalId(), domainEvent.email(), domainEvent.userType(), domainEvent.roles(),
        correlationId, causationId, traceMeta.traceId(), traceMeta.spanId(),
        PRODUCER, actorId, null);

    NotificationEvent notification = NotificationEvent.from(integrationEvent, domainEvent.externalId(), integrationEvent.aggregateType());
    OutboxEvent notificationOutbox = OutboxEvent.create(
        "notification.identity.user.created.v1",
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
        integrationEvent.specVersion(),
        JsonObject.mapFrom(notification).encode(),
        EventScope.EXTERNAL_ONLY,
        domainEvent.occurredOn());
    outboxEventRepository.save(notificationOutbox);
  }

  @WithSpan("identity.user.delete-persist")
  @Transactional
  public void deleteSyncRecord(String externalId, String correlationId) {
    UserSyncRecord record = syncRepository.findByExternalId(externalId);
    String email = record != null ? record.email() : null;
    String userType = record != null ? record.userType() : null;

    syncRepository.deleteByExternalId(externalId);

    EventMetadata traceMeta = traceContextProvider.current();
    UserDeletedEvent event = UserDeletedEvent.v1(
        externalId, correlationId, null, traceMeta.traceId(), traceMeta.spanId(),
        PRODUCER, null, null, email, userType);

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
        event.specVersion(),
        JsonObject.mapFrom(event).encode(),
        EventScope.BOTH,
        event.occurredOn());
    outboxEventRepository.save(outboxEvent);
    identityMetrics.recordDeleted();
  }
}
