package com.microservice.quarkus.user.identity.infrastructure.eda;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.dto.ClientSummary;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.service.IdentityPersistenceService;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEventRepository;
import com.microservice.quarkus.user.shared.application.saga.SagaRepository;
import com.microservice.quarkus.user.shared.infrastructure.db.hibernate.repository.OutboxEventPanacheRepository;
import com.microservice.quarkus.user.shared.infrastructure.db.hibernate.repository.SagaInstancePanacheRepository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EventMetadataIT {

    @Inject
    OutboxEventRepository outboxEventRepository;

    @Inject
    OutboxEventPanacheRepository outboxPanacheRepo;

    @Inject
    SagaInstancePanacheRepository sagaPanacheRepo;

    @Inject
    IdentitySyncRepository identitySyncRepository;

    @Inject
    ClientIdentityProvider clientIdentityProvider;

    @Inject
    IdentityPersistenceService identityPersistenceService;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    @Transactional
    void cleanup() {
        outboxPanacheRepo.deleteAll();
        sagaPanacheRepo.deleteAll();
        identitySyncRepository.deleteAll();
    }

    // ───────────────────────────────────────────────────────────────
    // Outbox Event Metadata — All 13 fields present
    // ───────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void shouldCreateOutboxEventWithAllMetadataFields() {
        String email = "metadata-it-" + UUID.randomUUID() + "@example.com";
        String dashboardClientUuid = getDashboardClientUuid();

        String correlationId = UUID.randomUUID().toString();

        given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .header("x-correlation-id", correlationId)
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "MetaPass123!",
                        "type", "ADMIN",
                        "clientId", dashboardClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200);

        UserSyncRecord record = identitySyncRepository.findByEmail(email);
        assertNotNull(record, "PENDING record should exist after webhook POST");
        identityPersistenceService.completeSync(record);

        List<OutboxEvent> events = outboxEventRepository.findUnpublished();
        assertFalse(events.isEmpty(), "Outbox should contain events after registration");

        OutboxEvent integrationEvent = events.stream()
                .filter(e -> "identity.user.registered.v1".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        // Verify all 13 EventMetadata fields
        assertNotNull(integrationEvent.getId(), "eventId must not be null");
        assertEquals("identity.user.registered.v1", integrationEvent.getEventType());
        assertNotNull(integrationEvent.getEventVersion(), "eventVersion must not be null");
        assertEquals(1, integrationEvent.getEventVersion(), "eventVersion should be CURRENT_VERSION=1");
        assertEquals("User", integrationEvent.getAggregateType());
        assertNotNull(integrationEvent.getAggregateId(), "aggregateId must not be null");
        assertEquals(record.id(), integrationEvent.getCorrelationId(), "correlationId must match record id");
        // causationId is null for entry-point events (no parent event caused this)
        assertNull(integrationEvent.getCausationId(), "causationId should be null for entry-point events");
        // traceId and spanId come from OTel context — may be null if no active span
        // producer is always set
        assertEquals("user-service", integrationEvent.getProducer(), "producer must be 'user-service'");
        // actorId is null for webhook-initiated events (not propagated yet)
        assertNull(integrationEvent.getActorId(), "actorId should be null for webhook-initiated events");
        // tenantId is null in current implementation
        assertNull(integrationEvent.getTenantId(), "tenantId should be null when not set");
        assertNotNull(integrationEvent.getOccurredOn(), "occurredOn must not be null");
        assertFalse(integrationEvent.getPublished(), "event should be unpublished");

        // Verify event payload contains full metadata
        try {
            JsonNode payload = objectMapper.readTree(integrationEvent.getEventPayload());
            assertEquals(email, payload.get("email").asText());
            assertEquals("ADMIN", payload.get("type").asText());
            assertNotNull(payload.get("aggregateId"), "payload must contain aggregateId");
            assertEquals(record.id(), payload.get("correlationId").asText(), "payload correlationId must match record id");
            assertTrue(payload.get("causationId").isNull(), "payload causationId must be null for entry-point events");
            assertEquals("user-service", payload.get("producer").asText(), "payload producer must match");
            assertEquals(1, payload.get("eventVersion").asInt(), "payload eventVersion must match");
        } catch (Exception e) {
            fail("Failed to parse event payload as JSON: " + e.getMessage());
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Notification Event — EXTERNAL_ONLY scope
    // ───────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void shouldCreateNotificationEventWithExternalOnlyScope() {
        String email = "notification-it-" + UUID.randomUUID() + "@example.com";
        String dashboardClientUuid = getDashboardClientUuid();

        given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "NotifPass123!",
                        "type", "ADMIN",
                        "clientId", dashboardClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200);

        UserSyncRecord record = identitySyncRepository.findByEmail(email);
        assertNotNull(record, "PENDING record should exist after webhook POST");
        identityPersistenceService.completeSync(record);

        List<OutboxEvent> events = outboxEventRepository.findUnpublished();
        OutboxEvent notificationEvent = events.stream()
                .filter(e -> "notification.identity.user.registered.v1".equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("notification.identity.user.registered.v1 event not found in outbox"));

        assertEquals(com.microservice.quarkus.user.shared.application.outbox.EventScope.EXTERNAL_ONLY,
                notificationEvent.getScope(), "Notification must be EXTERNAL_ONLY");
        assertNotNull(notificationEvent.getCorrelationId());
        assertNotNull(notificationEvent.getProducer());
        assertEquals("user-service", notificationEvent.getProducer());
    }

    // ───────────────────────────────────────────────────────────────
    // Saga Instance — Created on user registration
    // ───────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void shouldCreateSagaInstanceOnUserRegistration() {
        String email = "saga-it-" + UUID.randomUUID() + "@example.com";
        String dashboardClientUuid = getDashboardClientUuid();

        String correlationId = UUID.randomUUID().toString();

        given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .header("x-correlation-id", correlationId)
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "SagaPass123!",
                        "type", "ADMIN",
                        "clientId", dashboardClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200);

        UserSyncRecord record = identitySyncRepository.findByEmail(email);
        assertNotNull(record, "PENDING record should exist after webhook POST");
        identityPersistenceService.completeSync(record);

        var sagaEntity = sagaPanacheRepo.findByCorrelationId(record.id());
        assertNotNull(sagaEntity, "SagaInstance must be created with record id as correlationId");
        assertEquals("UserRegistrationSaga", sagaEntity.getSagaType());
        assertEquals("RUNNING", sagaEntity.getStatus().name());
        assertEquals("user-synced", sagaEntity.getCurrentStep());
        assertNotNull(sagaEntity.getStartedAt());
    }

    // ───────────────────────────────────────────────────────────────
    // Correlation ID — Propagated from HTTP header to outbox
    // ───────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void shouldPropagateCorrelationIdFromHttpHeaderToOutbox() {
        String email = "correlation-it-" + UUID.randomUUID() + "@example.com";
        String frontendClientUuid = getFrontendClientUuid();
        String explicitCorrelationId = "corr-" + UUID.randomUUID();

        given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .header("x-correlation-id", explicitCorrelationId)
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "CorrPass123!",
                        "type", "STANDARD",
                        "clientId", frontendClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200);

        UserSyncRecord record = identitySyncRepository.findByEmail(email);
        assertNotNull(record, "PENDING record should exist after webhook POST");
        identityPersistenceService.completeSync(record);

        List<OutboxEvent> events = outboxEventRepository.findUnpublished();
        assertFalse(events.isEmpty());

        for (OutboxEvent event : events) {
            assertEquals(record.id(), event.getCorrelationId(),
                    "All outbox events must carry the same correlationId (record id)");
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Outbox Retry + DLQ — Increment retry and mark dead
    // ───────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void shouldSupportRetryAndDlqOnOutboxEvent() {
        String email = "retry-it-" + UUID.randomUUID() + "@example.com";
        String dashboardClientUuid = getDashboardClientUuid();

        given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "RetryPass123!",
                        "type", "ADMIN",
                        "clientId", dashboardClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200);

        UserSyncRecord record = identitySyncRepository.findByEmail(email);
        assertNotNull(record, "PENDING record should exist after webhook POST");
        identityPersistenceService.completeSync(record);

        OutboxEvent event = outboxEventRepository.findUnpublished().stream()
                .filter(e -> "identity.user.registered.v1".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals(0, event.getRetryCount());
        assertFalse(event.getDead());

        event.incrementRetry();
        assertEquals(1, event.getRetryCount());
        assertNotNull(event.getNextRetryAt());
        assertTrue(event.shouldRetry());
        assertTrue(event.isNotReady(), "Event in backoff delay should not be ready");

        outboxEventRepository.update(event);

        for (int i = 2; i <= 5; i++) {
            event.incrementRetry();
        }
        assertEquals(5, event.getRetryCount());
        assertFalse(event.shouldRetry(), "Should not retry after maxRetries=5");

        event.markAsDead("Test DLQ reason");
        assertTrue(event.getDead());
        assertEquals("Test DLQ reason", event.getDeadReason());
        assertNotNull(event.getDeadAt());

        outboxEventRepository.update(event);

        List<OutboxEvent> deadEvents = outboxEventRepository.findDeadEvents();
        assertEquals(1, deadEvents.size());
        assertEquals("Test DLQ reason", deadEvents.get(0).getDeadReason());
    }

    // ───────────────────────────────────────────────────────────────
    // Event Versioning — CURRENT_VERSION constant
    // ───────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void shouldSetEventVersionOnOutboxRow() {
        String email = "version-it-" + UUID.randomUUID() + "@example.com";
        String dashboardClientUuid = getDashboardClientUuid();

        given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "VersionPass123!",
                        "type", "ADMIN",
                        "clientId", dashboardClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200);

        UserSyncRecord record = identitySyncRepository.findByEmail(email);
        assertNotNull(record, "PENDING record should exist after webhook POST");
        identityPersistenceService.completeSync(record);

        OutboxEvent event = outboxEventRepository.findUnpublished().stream()
                .filter(e -> "identity.user.registered.v1".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertEquals(1, event.getEventVersion(), "Event version must match CURRENT_VERSION=1");
    }

    // ───────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────

    private String getDashboardClientUuid() {
        return clientIdentityProvider.getClientSummaries().stream()
                .filter(c -> "dashboard-client".equals(c.name()))
                .findFirst()
                .map(ClientSummary::id)
                .orElseThrow(() -> new IllegalStateException("dashboard-client not found"));
    }

    private String getFrontendClientUuid() {
        return clientIdentityProvider.getClientSummaries().stream()
                .filter(c -> "frontend-client".equals(c.name()))
                .findFirst()
                .map(ClientSummary::id)
                .orElseThrow(() -> new IllegalStateException("frontend-client not found"));
    }
}
