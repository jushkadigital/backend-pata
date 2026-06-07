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
import com.microservice.quarkus.user.passenger.domain.Passenger;
import com.microservice.quarkus.user.passenger.domain.PassengerStatus;
import com.microservice.quarkus.user.passenger.domain.PassengerType;
import com.microservice.quarkus.user.shared.infrastructure.eventbus.OutboxEventPublisher;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end test: passenger self-registration full pipeline.
 *
 * Flow:
 *   1. POST /webhooks/payload  (passenger self-register)
 *   2. UserSyncRecord = PENDING in DB
 *   3. IdentityPersistenceService.completeSync()  →  SYNCED + outbox event
 *   4. OutboxEventPublisher.publishPendingEvents()  →  EventBus delivery
 *   5. UserCreatedListener  →  Passenger created in DB
 *   6. Verify passenger data (email, externalId, type, status)
 */
@QuarkusTest
class PassengerSelfRegistrationE2ETest {

    @Inject
    ClientIdentityProvider clientIdentityProvider;

    @Inject
    IdentitySyncRepository identitySyncRepository;

    @Inject
    IdentityPersistenceService identityPersistenceService;

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
    // Happy path: passenger self-register with "basic" role
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldSelfRegisterPassengerE2E() {
        String email = "passenger-e2e-" + UUID.randomUUID() + "@example.com";
        String frontendClientUuid = getFrontendClientUuid();

        // ── Step 1: Self-register via webhook ──
        String providerId = given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "PassengerE2E123!",
                        "type", "STANDARD",
                        "clientId", frontendClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200)
                .extract()
                .path("providerid");

        assertNotNull(providerId, "Keycloak user ID must be returned");
        assertFalse(providerId.isEmpty());

        // ── Step 2: Verify PENDING record in identity DB ──
        UserSyncRecord record = findSyncRecord(providerId);
        assertNotNull(record, "PENDING record must exist after webhook POST");
        assertEquals("PASSENGER", record.userType());
        assertEquals(List.of("basic"), record.roles());
        assertEquals(SyncStatus.PENDING, record.syncStatus());

        // ── Step 3: Trigger sync processor → SYNCED + outbox event ──
        completeSync(record);

        // ── Step 4: Trigger outbox publisher → EventBus delivery ──
        outboxEventPublisher.publishPendingEvents();

        // ── Step 5: Wait for UserCreatedListener → passenger in DB ──
        await().atMost(Duration.ofSeconds(5)).until(() ->
                dbHelper.findPassengerByExternalId(providerId).isPresent());

        // ── Step 6: Verify passenger data ──
        Passenger passenger = dbHelper.findPassengerByExternalId(providerId).orElseThrow();
        assertEquals(email, passenger.getEmail().value());
        assertEquals(providerId, passenger.getExternalId());
        assertEquals(PassengerType.BASIC, passenger.getType(),
                "clientRoles=['basic'] must map to PassengerType.BASIC");
        assertEquals(PassengerStatus.INCOMPLETE_PROFILE, passenger.getStatus(),
                "New passenger must have INCOMPLETE_PROFILE status");

        // Cleanup
        dbHelper.deleteTestPassengers(providerId);
    }

    // ───────────────────────────────────────────────────────────────
    // Premium passenger: "premium" role → PassengerType.PREMIUM
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldSelfRegisterPremiumPassengerE2E() {
        String email = "premium-e2e-" + UUID.randomUUID() + "@example.com";
        String frontendClientUuid = getFrontendClientUuid();

        // Use "role":"premium" to override the default "basic" for frontend-client
        String providerId = given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "PremiumE2E123!",
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
        assertEquals(PassengerType.PREMIUM, passenger.getType(),
                "clientRoles=['premium'] must map to PassengerType.PREMIUM");
        assertEquals(PassengerStatus.INCOMPLETE_PROFILE, passenger.getStatus());

        dbHelper.deleteTestPassengers(providerId);
    }

    // ───────────────────────────────────────────────────────────────
    // Admin via webhook: should NOT create a passenger
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldNotCreatePassengerForAdminSelfRegistration() {
        String email = "admin-e2e-nopassenger-" + UUID.randomUUID() + "@example.com";
        String dashboardClientUuid = getDashboardClientUuid();

        String providerId = given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "AdminE2E123!",
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

        await().atMost(Duration.ofSeconds(3)).pollDelay(Duration.ofSeconds(2)).until(() ->
                dbHelper.findPassengerByExternalId(providerId).isEmpty());

        assertTrue(dbHelper.findPassengerByExternalId(providerId).isEmpty(),
                "Passenger must NOT be created for ADMIN userType");
    }

    // ───────────────────────────────────────────────────────────────
    // Idempotent: same webhook twice → one passenger
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldBeIdempotentWhenSamePassengerRegistersTwice() {
        String email = "idempotent-e2e-" + UUID.randomUUID() + "@example.com";
        String frontendClientUuid = getFrontendClientUuid();
        String correlationId = UUID.randomUUID().toString();

        Map<String, Object> body = Map.of(
                "email", email,
                "password", "IdempotentE2E123!",
                "type", "STANDARD",
                "clientId", frontendClientUuid);

        String firstId = given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .header("x-correlation-id", correlationId)
                .contentType("application/json")
                .body(body)
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200)
                .extract()
                .path("providerid");

        String secondId = given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(body)
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200)
                .extract()
                .path("providerid");

        assertEquals(firstId, secondId, "Idempotent: same email must return same providerId");

        UserSyncRecord record = findSyncRecord(firstId);
        assertNotNull(record);

        completeSync(record);
        outboxEventPublisher.publishPendingEvents();

        await().atMost(Duration.ofSeconds(5)).until(() ->
                dbHelper.findPassengerByExternalId(firstId).isPresent());

        Passenger passenger = dbHelper.findPassengerByExternalId(firstId).orElseThrow();
        assertEquals(email, passenger.getEmail().value());
        assertEquals(PassengerStatus.INCOMPLETE_PROFILE, passenger.getStatus());

        dbHelper.deleteTestPassengers(firstId);
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
