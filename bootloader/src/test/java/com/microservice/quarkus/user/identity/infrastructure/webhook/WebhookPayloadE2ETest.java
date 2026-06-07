package com.microservice.quarkus.user.identity.infrastructure.webhook;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.dto.ClientSummary;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebhookPayloadE2ETest {

    @Inject
    ClientIdentityProvider clientIdentityProvider;

    @Inject
    IdentitySyncRepository identitySyncRepository;

    @BeforeEach
    @Transactional
    void cleanup() {
        identitySyncRepository.deleteAll();
    }

    // ───────────────────────────────────────────────────────────────
    // POST /webhooks/payload — Admin creation E2E
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldCreateAdminViaWebhookPayload() {
        String email = "admin-e2e-" + UUID.randomUUID() + "@example.com";
        String dashboardClientUuid = getDashboardClientUuid();

        given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "AdminPass123!",
                        "type", "ADMIN",
                        "clientId", dashboardClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200)
                .body("providerid", not(emptyString()));
    }

    @Test
    @Transactional
    void shouldCreateAdminInKeycloakAndDBViaWebhook() {
        String email = "admin-webhook-e2e-" + UUID.randomUUID() + "@example.com";
        String dashboardClientUuid = getDashboardClientUuid();

        String providerId = given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "WebhookPass123!",
                        "type", "ADMIN",
                        "clientId", dashboardClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200)
                .extract()
                .path("providerid");

        assertNotNull(providerId);
        assertFalse(providerId.isEmpty());

        UserSyncRecord record = identitySyncRepository.findByExternalId(providerId);
        assertNotNull(record);
        assertEquals(email, record.email());
        assertEquals("ADMIN", record.userType());
        assertEquals(List.of("editor"), record.roles());
        // Phased sync: webhook payload creates user in Keycloak first, then persists as PENDING.
        // SyncStatusProcessor will transition PENDING → SYNCED asynchronously.
        assertEquals(SyncStatus.PENDING, record.syncStatus());
    }

    @Test
    @Transactional
    void shouldCreatePassengerViaWebhookPayload() {
        String email = "passenger-webhook-e2e-" + UUID.randomUUID() + "@example.com";
        String frontendClientUuid = getFrontendClientUuid();

        String providerId = given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "PassengerPass123!",
                        "type", "STANDARD",
                        "clientId", frontendClientUuid))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(200)
                .extract()
                .path("providerid");

        assertNotNull(providerId);

        UserSyncRecord record = identitySyncRepository.findByExternalId(providerId);
        assertNotNull(record);
        assertEquals("PASSENGER", record.userType());
        assertEquals(List.of("basic"), record.roles());
    }

    // ───────────────────────────────────────────────────────────────
    // Idempotent consumer test
    // ───────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void shouldBeIdempotentWhenSameWebhookSentTwice() {
        String email = "idempotent-e2e-" + UUID.randomUUID() + "@example.com";
        String dashboardClientUuid = getDashboardClientUuid();

        Map<String, Object> body = Map.of(
                "email", email,
                "password", "IdempotentPass123!",
                "type", "ADMIN",
                "clientId", dashboardClientUuid);

        String firstId = given()
                .header("X-Webhook-Secret", "test-webhook-secret")
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

        assertEquals(firstId, secondId);
    }

    // ───────────────────────────────────────────────────────────────
    // Auth validation
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldRejectWebhookWithoutSecret() {
        given()
                .contentType("application/json")
                .body(Map.of(
                        "email", "no-secret@example.com",
                        "password", "Pass123!",
                        "type", "ADMIN",
                        "clientId", UUID.randomUUID()))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldRejectWebhookWithWrongSecret() {
        given()
                .header("X-Webhook-Secret", "wrong-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", "wrong-secret@example.com",
                        "password", "Pass123!",
                        "type", "ADMIN",
                        "clientId", UUID.randomUUID()))
                .when()
                .post("/webhooks/payload")
                .then()
                .statusCode(401);
    }

    // ───────────────────────────────────────────────────────────────
    // GET /keycloak/users — Verify after creation
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldListUsersAfterAdminCreation() {
        String email = "admin-list-e2e-" + UUID.randomUUID() + "@example.com";
        String dashboardClientUuid = getDashboardClientUuid();

        given()
                .header("X-Webhook-Secret", "test-webhook-secret")
                .contentType("application/json")
                .body(Map.of(
                        "email", email,
                        "password", "ListPass123!",
                        "type", "ADMIN",
                        "clientId", dashboardClientUuid))
                .post("/webhooks/payload");

        given()
                .when()
                .get("/keycloak/users")
                .then()
                .statusCode(200)
                .body(".", hasItem(email));
    }

    // ───────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────

    private String getDashboardClientUuid() {
        return clientIdentityProvider.getClientSummaries().stream()
                .filter(c -> "dashboard-client".equals(c.name()))
                .findFirst()
                .map(ClientSummary::id)
                .orElseThrow(() -> new IllegalStateException(
                        "dashboard-client not found — IdentityBootstrap may not have run"));
    }

    private String getFrontendClientUuid() {
        return clientIdentityProvider.getClientSummaries().stream()
                .filter(c -> "frontend-client".equals(c.name()))
                .findFirst()
                .map(ClientSummary::id)
                .orElseThrow(() -> new IllegalStateException(
                        "frontend-client not found — IdentityBootstrap may not have run"));
    }
}
