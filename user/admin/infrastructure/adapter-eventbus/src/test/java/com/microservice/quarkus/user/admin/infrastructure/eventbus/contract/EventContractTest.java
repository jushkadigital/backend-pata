package com.microservice.quarkus.user.admin.infrastructure.eventbus.contract;

import static org.junit.jupiter.api.Assertions.*;

import com.microservice.quarkus.user.admin.infrastructure.eventbus.acl.UserRegisteredEventDTO;
import com.microservice.quarkus.user.admin.infrastructure.eventbus.acl.UserDeletedEventDTO;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

/**
 * Contract tests: validates that the JSON produced by identity module's
 * IntegrationEvent records deserializes correctly into admin module DTOs.
 */
class EventContractTest {

    private String buildUserRegisteredEventJson() {
        return new JsonObject()
            .put("aggregateId", "ext-789")
            .put("aggregateType", "User")
            .put("eventType", "identity.user.registered.v1")
            .put("eventVersion", 1)
            .put("email", "admin@example.com")
            .put("type", "ADMIN")
            .put("subType", "STANDARD")
            .put("correlationId", "corr-3")
            .put("causationId", (String) null)
            .put("traceId", "trace-3")
            .put("spanId", "span-3")
            .put("producer", "user-service")
            .put("actorId", (String) null)
            .put("tenantId", (String) null)
            .put("eventId", UUID.randomUUID().toString())
            .put("occurredOn", Instant.now().toString())
            .encode();
    }

    @Test
    void userRegisteredEvent_shouldDeserializeIntoAdminDTO() {
        String json = buildUserRegisteredEventJson();
        UserRegisteredEventDTO dto = new JsonObject(json).mapTo(UserRegisteredEventDTO.class);

        assertEquals("ext-789", dto.aggregateId());
        assertEquals("User", dto.aggregateType());
        assertEquals("identity.user.registered.v1", dto.eventType());
        assertEquals(1, dto.eventVersion());
        assertEquals("admin@example.com", dto.email());
        assertEquals("ADMIN", dto.type());
        assertEquals("corr-3", dto.correlationId());
        assertNull(dto.causationId());
        assertEquals("trace-3", dto.traceId());
        assertEquals("span-3", dto.spanId());
        assertEquals("user-service", dto.producer());
        assertNull(dto.actorId());
        assertNull(dto.tenantId());
        assertNotNull(dto.eventId());
        assertNotNull(dto.occurredOn());
    }

    @Test
    void userDeletedEvent_shouldDeserializeIntoDeletedDTO() {
        String json = new JsonObject()
            .put("aggregateId", "ext-999")
            .put("aggregateType", "User")
            .put("eventType", "identity.user.deleted.v1")
            .put("eventVersion", 1)
            .put("correlationId", "corr-4")
            .put("causationId", (String) null)
            .put("traceId", "trace-4")
            .put("spanId", "span-4")
            .put("producer", "user-service")
            .put("actorId", (String) null)
            .put("tenantId", (String) null)
            .put("eventId", UUID.randomUUID().toString())
            .put("occurredOn", Instant.now().toString())
            .encode();

        UserDeletedEventDTO dto = new JsonObject(json).mapTo(UserDeletedEventDTO.class);

        assertEquals("ext-999", dto.aggregateId());
        assertEquals("User", dto.aggregateType());
        assertEquals("identity.user.deleted.v1", dto.eventType());
        assertEquals(1, dto.eventVersion());
        assertEquals("corr-4", dto.correlationId());
        assertNull(dto.causationId());
        assertEquals("trace-4", dto.traceId());
        assertEquals("span-4", dto.spanId());
        assertEquals("user-service", dto.producer());
        assertNull(dto.actorId());
        assertNull(dto.tenantId());
        assertNotNull(dto.eventId());
        assertNotNull(dto.occurredOn());
    }
}
