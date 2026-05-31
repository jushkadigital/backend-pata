package com.microservice.quarkus.user.admin.application.service;

import com.microservice.quarkus.user.admin.application.dto.CreateAdminCommand;
import com.microservice.quarkus.user.admin.application.event.AdminRegisteredEvent;
import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo.AdminEntity;
import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.repository.AdminPanacheRepository;
import com.microservice.quarkus.user.shared.application.NotificationEvent;
import com.microservice.quarkus.user.shared.domain.EmailAddress;
import com.microservice.quarkus.user.shared.application.outbox.EventMetadata;
import com.microservice.quarkus.user.shared.application.outbox.EventScope;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEventRepository;
import com.microservice.quarkus.user.shared.application.outbox.TraceContextProvider;

import io.vertx.core.json.JsonObject;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class AdminService {

    private static final String PRODUCER = "user-service";

    private final AdminPanacheRepository repository;
    private final OutboxEventRepository outboxEventRepository;
    private final TraceContextProvider traceContextProvider;
    private final AdminMetrics adminMetrics;

    public AdminService(AdminPanacheRepository repository,
                        OutboxEventRepository outboxEventRepository,
                        TraceContextProvider traceContextProvider,
                        AdminMetrics adminMetrics) {
        this.repository = repository;
        this.outboxEventRepository = outboxEventRepository;
        this.traceContextProvider = traceContextProvider;
        this.adminMetrics = adminMetrics;
    }

    @Transactional
    public String register(CreateAdminCommand cmd) {
        return register(cmd, null, null, null);
    }

    @Transactional
    public String register(CreateAdminCommand cmd, String correlationId) {
        return register(cmd, correlationId, null, null);
    }

    @WithSpan("admin.create")
    @Transactional
    public String register(CreateAdminCommand cmd, String correlationId, String causationId, String actorId) {
        AdminEntity admin = new AdminEntity();
        admin.setId(UUID.randomUUID().toString());
        admin.setExternalId(cmd.externalId());
        admin.setEmail(new EmailAddress(cmd.email()));
        admin.setType(cmd.type().toUpperCase());
        admin.setCreatedAt(Instant.now());
        admin.setUpdatedAt(Instant.now());
        repository.persist(admin);

        String effectiveCorrelationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        EventMetadata traceMeta = traceContextProvider.current();

        AdminRegisteredEvent event = new AdminRegisteredEvent(cmd.externalId(), cmd.email(), cmd.type(),
            effectiveCorrelationId, causationId, traceMeta.traceId(), traceMeta.spanId(),
            PRODUCER, actorId, null);
        OutboxEvent outboxEvent = OutboxEvent.create(
            event.eventType(),
            event.eventVersion(),
            event.aggregateType(),
            admin.getId(),
            effectiveCorrelationId,
            causationId,
            traceMeta.traceId(),
            traceMeta.spanId(),
            PRODUCER,
            actorId,
            null,
            JsonObject.mapFrom(event).encode(),
            EventScope.EXTERNAL_ONLY, event.occurredOn());
        outboxEventRepository.save(outboxEvent);

        NotificationEvent notification = NotificationEvent.from(event, admin.getId(), event.aggregateType());
        OutboxEvent notificationOutbox = OutboxEvent.create(
            "notification.admin.registered.v1",
            notification.eventVersion(),
            notification.aggregateType(),
            admin.getId(),
            effectiveCorrelationId,
            causationId,
            traceMeta.traceId(),
            traceMeta.spanId(),
            PRODUCER,
            actorId,
            null,
            JsonObject.mapFrom(notification).encode(),
            EventScope.EXTERNAL_ONLY,
            event.occurredOn());
        outboxEventRepository.save(notificationOutbox);
        adminMetrics.recordCreated();

        return admin.getId();
    }

    @WithSpan("admin.delete")
    @Transactional
    public void deleteByExternalId(String externalId) {
        repository.deleteByExternalId(externalId);
        adminMetrics.recordDeleted();
    }
}
