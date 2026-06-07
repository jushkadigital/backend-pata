package com.microservice.quarkus.user.integration;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.dto.ClientSummary;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.service.IdentityPersistenceService;
import com.microservice.quarkus.user.identity.application.service.UserService;
import com.microservice.quarkus.user.passenger.domain.Passenger;
import com.microservice.quarkus.user.passenger.domain.PassengerType;
import com.microservice.quarkus.user.passenger.domain.PassengerStatus;
import com.microservice.quarkus.user.shared.infrastructure.eventbus.OutboxEventPublisher;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end test: user deletion cascades to passenger subdomain.
 *
 * Flow:
 *   1. POST /webhooks/payload  (passenger self-register)
 *   2. completeSync() → outbox → EventBus → passenger created in DB
 *   3. userService.deleteByExternalId()  →  sync record deleted + outbox event
 *   4. OutboxEventPublisher.publishPendingEvents()  →  EventBus delivery
 *   5. UserDeletedListener  →  passenger deleted from DB
 *   6. Verify passenger no longer exists
 */
@QuarkusTest
class UserDeletedE2ETest {

    @Inject
    ClientIdentityProvider clientIdentityProvider;

    @Inject
    IdentitySyncRepository identitySyncRepository;

    @Inject
    IdentityPersistenceService identityPersistenceService;

    @Inject
    UserService userService;

    @Inject
    OutboxEventPublisher outboxEventPublisher;

    @Inject
    TestDbHelper dbHelper;

    @BeforeEach
    @Transactional
    void cleanup() {
        identitySyncRepository.deleteAll();
    }

    // ───────────────────────────────────────────────────────────────
    // Happy path: passenger created then deleted → passenger removed from DB
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldDeletePassengerWhenUserDeletedE2E() {
        String email = "delete-e2e-" + UUID.randomUUID() + "@example.com";
        String frontendClientUuid = getFrontendClientUuid();

        // ── Step 1: Self-register a passenger via webhook ──
        String providerId = given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "DeleteE2E123!",
                        "type", "STANDARD",
                        "clientId", frontendClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200)
                .extract()
                .path("providerid");

        assertNotNull(providerId, "Keycloak user ID must be returned");

        // ── Step 2: Complete sync → outbox → EventBus → passenger in DB ──
        UserSyncRecord record = findSyncRecord(providerId);
        assertNotNull(record, "PENDING record must exist after webhook POST");
        assertEquals("PASSENGER", record.userType());
        assertEquals(SyncStatus.PENDING, record.syncStatus());

        completeSync(record);
        outboxEventPublisher.publishPendingEvents();

        await().atMost(Duration.ofSeconds(5)).until(() ->
                dbHelper.findPassengerByExternalId(providerId).isPresent());

        Passenger passenger = dbHelper.findPassengerByExternalId(providerId).orElseThrow();
        assertEquals(email, passenger.getEmail().value());
        assertEquals(PassengerType.BASIC, passenger.getType());

        // ── Step 3: Delete user via userService (simulates KeycloakWebhookConsumer DELETE flow) ──
        String correlationId = UUID.randomUUID().toString();
        deleteUser(providerId, correlationId);

        // ── Step 4: Publish outbox event → EventBus delivery ──
        outboxEventPublisher.publishPendingEvents();

        // ── Step 5: Wait for UserDeletedListener → passenger removed from DB ──
        await().atMost(Duration.ofSeconds(5)).until(() ->
                dbHelper.findPassengerByExternalId(providerId).isEmpty());

        // ── Step 6: Verify passenger is gone ──
        assertTrue(dbHelper.findPassengerByExternalId(providerId).isEmpty(),
                "Passenger must be deleted from DB after user deletion event");

        // Verify sync record is also gone
        assertNull(findSyncRecord(providerId),
                "Identity sync record must be deleted");
    }

    // ───────────────────────────────────────────────────────────────
    // Premium passenger created then deleted → passenger removed from DB
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldDeletePremiumPassengerWhenUserDeletedE2E() {
        String email = "delete-premium-e2e-" + UUID.randomUUID() + "@example.com";
        String frontendClientUuid = getFrontendClientUuid();

        // Create premium passenger
        String providerId = given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "DeletePremiumE2E123!",
                        "role", "premium",
                        "clientId", frontendClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200)
                .extract()
                .path("providerid");

        assertNotNull(providerId);

        UserSyncRecord record = findSyncRecord(providerId);
        assertNotNull(record);
        assertEquals("PASSENGER", record.userType());

        completeSync(record);
        outboxEventPublisher.publishPendingEvents();

        await().atMost(Duration.ofSeconds(5)).until(() ->
                dbHelper.findPassengerByExternalId(providerId).isPresent());

        Passenger passenger = dbHelper.findPassengerByExternalId(providerId).orElseThrow();
        assertEquals(PassengerType.PREMIUM, passenger.getType());

        // Delete user
        deleteUser(providerId, UUID.randomUUID().toString());
        outboxEventPublisher.publishPendingEvents();

        await().atMost(Duration.ofSeconds(5)).until(() ->
                dbHelper.findPassengerByExternalId(providerId).isEmpty());

        assertTrue(dbHelper.findPassengerByExternalId(providerId).isEmpty(),
                "Premium passenger must be deleted from DB after user deletion event");
    }

    // ───────────────────────────────────────────────────────────────
    // Admin deletion: no passenger record exists, no cascade needed
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldNotFailWhenDeletingAdminUserWithNoPassenger() {
        String email = "admin-delete-e2e-" + UUID.randomUUID() + "@example.com";
        String dashboardClientUuid = getDashboardClientUuid();

        // Create an admin user (no passenger created)
        String providerId = given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "AdminDeleteE2E123!",
                        "type", "ADMIN",
                        "clientId", dashboardClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200)
                .extract()
                .path("providerid");

        assertNotNull(providerId);

        UserSyncRecord record = findSyncRecord(providerId);
        assertNotNull(record);
        assertEquals("ADMIN", record.userType());

        completeSync(record);
        outboxEventPublisher.publishPendingEvents();

        // Confirm no passenger was created for this admin
        assertTrue(dbHelper.findPassengerByExternalId(providerId).isEmpty(),
                "No passenger should exist for ADMIN user");

        // Delete the admin user
        deleteUser(providerId, UUID.randomUUID().toString());
        outboxEventPublisher.publishPendingEvents();

        // Verify: no exception thrown, sync record deleted, still no passenger
        assertNull(findSyncRecord(providerId),
                "Admin sync record must be deleted");
        assertTrue(dbHelper.findPassengerByExternalId(providerId).isEmpty(),
                "No passenger should exist after admin deletion either");
    }

    // ───────────────────────────────────────────────────────────────
    // Idempotent deletion: same delete event twice → no error
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldBeIdempotentWhenSameUserDeletedTwice() {
        String email = "delete-idempotent-e2e-" + UUID.randomUUID() + "@example.com";
        String frontendClientUuid = getFrontendClientUuid();

        // Create passenger
        String providerId = given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "DeleteIdempotentE2E123!",
                        "type", "STANDARD",
                        "clientId", frontendClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200)
                .extract()
                .path("providerid");

        assertNotNull(providerId);

        UserSyncRecord record = findSyncRecord(providerId);
        assertNotNull(record);

        completeSync(record);
        outboxEventPublisher.publishPendingEvents();

        await().atMost(Duration.ofSeconds(5)).until(() ->
                dbHelper.findPassengerByExternalId(providerId).isPresent());

        // First delete
        deleteUser(providerId, UUID.randomUUID().toString());
        outboxEventPublisher.publishPendingEvents();

        await().atMost(Duration.ofSeconds(5)).until(() ->
                dbHelper.findPassengerByExternalId(providerId).isEmpty());

        // Second delete (sync record already gone — userService.deleteByExternalId
        // will hit syncRepository.findByExternalId returning null, but still creates outbox event)
        // This tests idempotency of the full pipeline
        String secondCorrelationId = UUID.randomUUID().toString();
        deleteUser(providerId, secondCorrelationId);
        outboxEventPublisher.publishPendingEvents();

        // No exception, passenger still absent
        assertTrue(dbHelper.findPassengerByExternalId(providerId).isEmpty(),
                "Passenger must remain deleted after second deletion event");
    }

    // ───────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────

    @Transactional
    UserSyncRecord findSyncRecord(String externalId) {
        return identitySyncRepository.findByExternalId(externalId);
    }

    @Transactional
    void completeSync(UserSyncRecord record) {
        identityPersistenceService.completeSync(record);
    }

    @Transactional
    void deleteUser(String externalId, String correlationId) {
        userService.deleteByExternalId(externalId, correlationId);
    }

    private String getFrontendClientUuid() {
        return clientIdentityProvider.getClientSummaries().stream()
                .filter(c -> "frontend-client".equals(c.name()))
                .findFirst()
                .map(ClientSummary::id)
                .orElseThrow(() -> new IllegalStateException(
                        "frontend-client not found — IdentityBootstrap may not have run"));
    }

    private String getDashboardClientUuid() {
        return clientIdentityProvider.getClientSummaries().stream()
                .filter(c -> "dashboard-client".equals(c.name()))
                .findFirst()
                .map(ClientSummary::id)
                .orElseThrow(() -> new IllegalStateException(
                        "dashboard-client not found — IdentityBootstrap may not have run"));
    }
}
