package com.microservice.quarkus.user.passenger.infrastructure.eventbus.contract;

import static org.junit.jupiter.api.Assertions.*;

import com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl.UserCreatedEventDTO;
import com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl.UserDeletedEventDTO;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contract tests: validates that the JSON produced by identity module's
 * IntegrationEvent records deserializes correctly into passenger module DTOs.
 */
class EventContractTest {

    private String buildUserCreatedEventJson() {
        return new JsonObject()
            .put("aggregateId", "ext-123")
            .put("aggregateType", "User")
            .put("eventType", "identity.user.created.v1")
            .put("eventVersion", 1)
            .put("email", "user@example.com")
            .put("userType", "PASSENGER")
            .put("clientRoles", List.of("basic"))
            .put("correlationId", "corr-1")
            .put("causationId", (String) null)
            .put("traceId", "trace-1")
            .put("spanId", "span-1")
            .put("producer", "user-service")
            .put("actorId", (String) null)
            .put("tenantId", (String) null)
            .put("eventId", UUID.randomUUID().toString())
            .put("occurredOn", Instant.now().toString())
            .encode();
    }

    @Test
    void userCreatedEvent_shouldDeserializeIntoPassengerDTO() {
        String json = buildUserCreatedEventJson();
        UserCreatedEventDTO dto = new JsonObject(json).mapTo(UserCreatedEventDTO.class);

        assertEquals("ext-123", dto.aggregateId());
        assertEquals("User", dto.aggregateType());
        assertEquals("identity.user.created.v1", dto.eventType());
        assertEquals(1, dto.eventVersion());
        assertEquals("user@example.com", dto.email());
        assertEquals("PASSENGER", dto.userType());
        assertEquals(List.of("basic"), dto.clientRoles());
        assertEquals("corr-1", dto.correlationId());
        assertNull(dto.causationId());
        assertEquals("trace-1", dto.traceId());
        assertEquals("span-1", dto.spanId());
        assertEquals("user-service", dto.producer());
        assertNull(dto.actorId());
        assertNull(dto.tenantId());
        assertNotNull(dto.eventId());
        assertNotNull(dto.occurredOn());
    }

    @Test
    void userDeletedEvent_shouldDeserializeIntoDeletedDTO() {
        String json = new JsonObject()
            .put("aggregateId", "ext-456")
            .put("aggregateType", "User")
            .put("eventType", "identity.user.deleted.v1")
            .put("eventVersion", 1)
            .put("correlationId", "corr-2")
            .put("causationId", (String) null)
            .put("traceId", "trace-2")
            .put("spanId", "span-2")
            .put("producer", "user-service")
            .put("actorId", (String) null)
            .put("tenantId", (String) null)
            .put("eventId", UUID.randomUUID().toString())
            .put("occurredOn", Instant.now().toString())
            .put("email", "deleted@example.com")
            .put("userType", "PASSENGER")
            .encode();

        UserDeletedEventDTO dto = new JsonObject(json).mapTo(UserDeletedEventDTO.class);

        assertEquals("ext-456", dto.aggregateId());
        assertEquals("User", dto.aggregateType());
        assertEquals("identity.user.deleted.v1", dto.eventType());
        assertEquals(1, dto.eventVersion());
        assertEquals("corr-2", dto.correlationId());
        assertNull(dto.causationId());
        assertEquals("trace-2", dto.traceId());
        assertEquals("span-2", dto.spanId());
        assertEquals("user-service", dto.producer());
        assertNull(dto.actorId());
        assertNull(dto.tenantId());
        assertNotNull(dto.eventId());
        assertNotNull(dto.occurredOn());
        assertEquals("deleted@example.com", dto.email());
        assertEquals("PASSENGER", dto.userType());
    }
}
