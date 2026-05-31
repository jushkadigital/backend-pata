package com.microservice.quarkus.user.passenger.infrastructure.eventbus.contract;

import static org.junit.jupiter.api.Assertions.*;

import com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl.UserRegisteredEventDTO;
import com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl.UserDeletedEventDTO;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

/**
 * Contract tests: validates that the JSON produced by identity module's
 * IntegrationEvent records deserializes correctly into passenger module DTOs.
 */
class EventContractTest {

    private String buildUserRegisteredEventJson() {
        return new JsonObject()
            .put("aggregateId", "ext-123")
            .put("aggregateType", "User")
            .put("eventType", "identity.user.registered.v1")
            .put("eventVersion", 1)
            .put("email", "user@example.com")
            .put("type", "PASSENGER")
            .put("subType", "STANDARD")
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
    void userRegisteredEvent_shouldDeserializeIntoPassengerDTO() {
        String json = buildUserRegisteredEventJson();
        UserRegisteredEventDTO dto = new JsonObject(json).mapTo(UserRegisteredEventDTO.class);

        assertEquals("ext-123", dto.aggregateId());
        assertEquals("User", dto.aggregateType());
        assertEquals("identity.user.registered.v1", dto.eventType());
        assertEquals(1, dto.eventVersion());
        assertEquals("user@example.com", dto.email());
        assertEquals("PASSENGER", dto.type());
        assertEquals("STANDARD", dto.subType());
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
    }
}
