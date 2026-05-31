package com.microservice.quarkus.user.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo.AdminEntity;
import com.microservice.quarkus.user.passenger.domain.Passenger;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EventBusIntegrationTest {

    @Inject
    io.vertx.mutiny.core.eventbus.EventBus eventBus;

    @Inject
    TestDbHelper dbHelper;

    @BeforeEach
    void cleanup() {
        dbHelper.deleteTestPassengers("ext-it-passenger-1", "ext-it-passenger-2");
        dbHelper.deleteTestAdmins("ext-it-admin-1", "ext-it-admin-2");
    }

    // ───────────────────────────────────────────────────────────────
    // UserRegisteredEvent → PASSENGER
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldCreatePassengerWhenUserRegisteredEventPublished() {
        String event = buildUserRegisteredEventJson(
            "ext-it-passenger-1", "passenger-it@example.com", "PASSENGER", "STANDARD");

        eventBus.publish("identity.user.registered", event);

        await().atMost(Duration.ofSeconds(5)).until(() ->
            dbHelper.findPassengerByExternalId("ext-it-passenger-1").isPresent());

        Passenger passenger = dbHelper.findPassengerByExternalId("ext-it-passenger-1").orElseThrow();
        assertEquals("passenger-it@example.com", passenger.getEmail().value());
        assertEquals("ext-it-passenger-1", passenger.getExternalId());
    }

    @Test
    void shouldNotCreatePassengerForAdminTypeEvent() {
        String event = buildUserRegisteredEventJson(
            "ext-it-admin-1", "admin-it@example.com", "ADMIN", "STANDARD");

        eventBus.publish("identity.user.registered", event);

        await().atMost(Duration.ofSeconds(3)).pollDelay(Duration.ofSeconds(1)).until(() ->
            dbHelper.findPassengerByExternalId("ext-it-admin-1").isEmpty());
    }

    @Test
    void shouldSkipIdempotentlyWhenSamePassengerEventPublishedTwice() {
        String event = buildUserRegisteredEventJson(
            "ext-it-passenger-2", "idempotent-it@example.com", "PASSENGER", "STANDARD");

        eventBus.publish("identity.user.registered", event);
        eventBus.publish("identity.user.registered", event);

        await().atMost(Duration.ofSeconds(5)).until(() ->
            dbHelper.findPassengerByExternalId("ext-it-passenger-2").isPresent());
    }

    // ───────────────────────────────────────────────────────────────
    // UserRegisteredEvent → ADMIN
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldCreateAdminWhenUserRegisteredEventPublished() {
        String event = buildUserRegisteredEventJson(
            "ext-it-admin-2", "admin-it2@example.com", "ADMIN", "STANDARD");

        eventBus.publish("identity.user.registered", event);

        await().atMost(Duration.ofSeconds(5)).until(() ->
            dbHelper.findAdminByExternalId("ext-it-admin-2") != null);

        AdminEntity admin = dbHelper.findAdminByExternalId("ext-it-admin-2");
        assertNotNull(admin);
        assertEquals("admin-it2@example.com", admin.getEmail().value());
        assertEquals("ext-it-admin-2", admin.getExternalId());
    }

    @Test
    void shouldNotCreateAdminForPassengerTypeEvent() {
        String event = buildUserRegisteredEventJson(
            "ext-it-passenger-2", "passenger-it2@example.com", "PASSENGER", "STANDARD");

        eventBus.publish("identity.user.registered", event);

        await().atMost(Duration.ofSeconds(3)).pollDelay(Duration.ofSeconds(1)).until(() ->
            dbHelper.findAdminByExternalId("ext-it-passenger-2") == null);
    }

    // ───────────────────────────────────────────────────────────────
    // UserDeletedEvent → PASSENGER DELETED
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldDeletePassengerWhenUserDeletedEventPublished() {
        String registerEvent = buildUserRegisteredEventJson(
            "ext-it-passenger-1", "delete-it@example.com", "PASSENGER", "STANDARD");
        eventBus.publish("identity.user.registered", registerEvent);

        await().atMost(Duration.ofSeconds(5)).until(() ->
            dbHelper.findPassengerByExternalId("ext-it-passenger-1").isPresent());

        String deleteEvent = buildUserDeletedEventJson("ext-it-passenger-1");
        eventBus.publish("identity.user.deleted", deleteEvent);

        await().atMost(Duration.ofSeconds(5)).until(() ->
            dbHelper.findPassengerByExternalId("ext-it-passenger-1").isEmpty());
    }

    // ───────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────

    private String buildUserRegisteredEventJson(String aggregateId, String email, String type, String subType) {
        return new JsonObject()
            .put("eventType", "identity.user.registered.v1")
            .put("aggregateType", "User")
            .put("aggregateId", aggregateId)
            .put("eventVersion", 1)
            .put("email", email)
            .put("type", type)
            .put("subType", subType)
            .put("correlationId", UUID.randomUUID().toString())
            .put("causationId", (String) null)
            .put("traceId", UUID.randomUUID().toString())
            .put("spanId", UUID.randomUUID().toString())
            .put("producer", "user-service")
            .put("actorId", (String) null)
            .put("tenantId", (String) null)
            .put("eventId", UUID.randomUUID().toString())
            .put("occurredOn", Instant.now().toString())
            .encode();
    }

    private String buildUserDeletedEventJson(String aggregateId) {
        return new JsonObject()
            .put("eventType", "identity.user.deleted.v1")
            .put("aggregateType", "User")
            .put("aggregateId", aggregateId)
            .put("eventVersion", 1)
            .put("correlationId", UUID.randomUUID().toString())
            .put("causationId", (String) null)
            .put("traceId", UUID.randomUUID().toString())
            .put("spanId", UUID.randomUUID().toString())
            .put("producer", "user-service")
            .put("actorId", (String) null)
            .put("tenantId", (String) null)
            .put("eventId", UUID.randomUUID().toString())
            .put("occurredOn", Instant.now().toString())
            .encode();
    }
}
