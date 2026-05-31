package com.microservice.quarkus.user.passenger.infrastructure.eventbus.consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.microservice.quarkus.user.passenger.application.service.PassengerService;
import com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl.UserDeletedEventDTO;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeletedListenerTest {

    @Mock
    PassengerService passengerService;

    @Mock
    Tracer tracer;

    @Mock
    SpanBuilder spanBuilder;

    @Mock
    Span span;

    @Mock
    Scope scope;

    private UserDeletedListener createListener() {
        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setParent(any())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.makeCurrent()).thenReturn(scope);
        UserDeletedListener listener = new UserDeletedListener(passengerService);
        listener.tracer = tracer;
        return listener;
    }

    @Test
    void onUserDeleted_shouldDeletePassengerByExternalId() {
        String externalId = UUID.randomUUID().toString();
        UserDeletedEventDTO dto = new UserDeletedEventDTO(externalId, "User", "identity.user.deleted.v1", 1,
            "corr-1", null, "trace-1", "span-1", "user-service", null, null,
            UUID.randomUUID(), java.time.Instant.now());
        String event = JsonObject.mapFrom(dto).encode();

        createListener().onUserDeleted(event);

        verify(passengerService).deleteByExternalId(externalId);
    }

    @Test
    void onUserDeleted_shouldPropagateExceptionOnFailure() {
        String externalId = UUID.randomUUID().toString();
        UserDeletedEventDTO dto = new UserDeletedEventDTO(externalId, "User", "identity.user.deleted.v1", 1,
            "corr-2", null, "trace-2", "span-2", "user-service", null, null,
            UUID.randomUUID(), java.time.Instant.now());
        String event = JsonObject.mapFrom(dto).encode();

        doThrow(new RuntimeException("DB down")).when(passengerService).deleteByExternalId(anyString());

        assertThrows(RuntimeException.class, () -> createListener().onUserDeleted(event));
    }
}
