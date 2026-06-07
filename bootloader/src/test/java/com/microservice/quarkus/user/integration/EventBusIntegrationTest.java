package com.microservice.quarkus.user.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
    }

    // ───────────────────────────────────────────────────────────────
    // UserCreatedEvent → PASSENGER
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldCreatePassengerWhenUserCreatedEventPublished() {
        String event = buildUserCreatedEventJson(
            "ext-it-passenger-1", "passenger-it@example.com", "PASSENGER", List.of("basic"));

        eventBus.publish("identity.user.created.v1", event);

        await().atMost(Duration.ofSeconds(5)).until(() ->
            dbHelper.findPassengerByExternalId("ext-it-passenger-1").isPresent());

        Passenger passenger = dbHelper.findPassengerByExternalId("ext-it-passenger-1").orElseThrow();
        assertEquals("passenger-it@example.com", passenger.getEmail().value());
        assertEquals("ext-it-passenger-1", passenger.getExternalId());
    }

    @Test
    void shouldNotCreatePassengerForAdminRoleEvent() {
        String event = buildUserCreatedEventJson(
            "ext-it-admin-1", "admin-it@example.com", "ADMIN", List.of("editor"));

        eventBus.publish("identity.user.created.v1", event);

        await().atMost(Duration.ofSeconds(3)).pollDelay(Duration.ofSeconds(1)).until(() ->
            dbHelper.findPassengerByExternalId("ext-it-admin-1").isEmpty());
    }

    @Test
    void shouldSkipIdempotentlyWhenSamePassengerEventPublishedTwice() {
        String event = buildUserCreatedEventJson(
            "ext-it-passenger-2", "idempotent-it@example.com", "PASSENGER", List.of("basic"));

        eventBus.publish("identity.user.created.v1", event);
        eventBus.publish("identity.user.created.v1", event);

        await().atMost(Duration.ofSeconds(5)).until(() ->
            dbHelper.findPassengerByExternalId("ext-it-passenger-2").isPresent());
    }

    // ───────────────────────────────────────────────────────────────
    // UserDeletedEvent → PASSENGER DELETED
    // ───────────────────────────────────────────────────────────────

    @Test
    void shouldDeletePassengerWhenUserDeletedEventPublished() {
        String registerEvent = buildUserCreatedEventJson(
            "ext-it-passenger-1", "delete-it@example.com", "PASSENGER", List.of("basic"));
        eventBus.publish("identity.user.created.v1", registerEvent);

        await().atMost(Duration.ofSeconds(5)).until(() ->
            dbHelper.findPassengerByExternalId("ext-it-passenger-1").isPresent());

        String deleteEvent = buildUserDeletedEventJson("ext-it-passenger-1");
        eventBus.publish("identity.user.deleted.v1", deleteEvent);

        await().atMost(Duration.ofSeconds(5)).until(() ->
            dbHelper.findPassengerByExternalId("ext-it-passenger-1").isEmpty());
    }

    // ───────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────

    private String buildUserCreatedEventJson(String aggregateId, String email, String userType, List<String> clientRoles) {
        return new JsonObject()
            .put("eventType", "identity.user.created.v1")
            .put("aggregateType", "User")
            .put("aggregateId", aggregateId)
            .put("eventVersion", 1)
            .put("email", email)
            .put("userType", userType)
            .put("clientRoles", clientRoles)
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
