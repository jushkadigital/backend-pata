package com.microservice.quarkus.user.passenger.infrastructure.eventbus.consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.microservice.quarkus.user.passenger.application.dto.CreatePassengerCommand;
import com.microservice.quarkus.user.passenger.application.service.PassengerService;
import com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl.UserCreatedEventDTO;
import com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl.UserCreatedEventMapper;
import com.microservice.quarkus.user.shared.application.outbox.IdempotencyStore;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCreatedListenerTest {

    @Mock
    PassengerService passengerService;

    @Mock
    UserCreatedEventMapper eventMapper;

    @Mock
    IdempotencyStore idempotencyStore;

    @Mock
    Tracer tracer;

    @Mock
    SpanBuilder spanBuilder;

    @Mock
    Span span;

    @Mock
    Scope scope;

    private UserCreatedListener createListener() {
        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setParent(any())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.makeCurrent()).thenReturn(scope);
        lenient().when(idempotencyStore.tryAcquire(anyString(), anyString())).thenReturn(true);
        UserCreatedListener listener = new UserCreatedListener(passengerService, eventMapper, idempotencyStore);
        listener.tracer = tracer;
        return listener;
    }

    private String buildEventJson(String aggregateId, String email, String userType, List<String> clientRoles) {
        UserCreatedEventDTO dto = new UserCreatedEventDTO(
            aggregateId, "User", "identity.user.created.v1", 1,
            email, userType, clientRoles,
            "corr-" + UUID.randomUUID(), null, "trace-1", "span-1",
            "user-service", null, null,
            UUID.randomUUID(), Instant.now());
        return JsonObject.mapFrom(dto).encode();
    }

    @Test
    void onUserCreated_shouldCreatePassengerForPassengerRoleEvent() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "user@example.com", "PASSENGER", List.of("basic"));

        CreatePassengerCommand cmd = new CreatePassengerCommand(externalId, "user@example.com", "STANDARD");
        when(eventMapper.toCommand(any(UserCreatedEventDTO.class))).thenReturn(cmd);

        createListener().onUserCreated(event);

        verify(passengerService).register(eq(cmd), anyString());
    }

    @Test
    void onUserCreated_shouldIgnoreNonPassengerRoleEvent() {
        String event = buildEventJson(UUID.randomUUID().toString(), "admin@example.com", "ADMIN", List.of("basic"));

        createListener().onUserCreated(event);

        verify(passengerService, never()).register(any(), anyString());
        verify(eventMapper, never()).toCommand(any());
    }

    @Test
    void onUserCreated_shouldSkipIdempotentlyIfPassengerAlreadyExists() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "existing@example.com", "PASSENGER", List.of("basic"));

        UserCreatedListener listener = createListener();
        reset(idempotencyStore);
        when(idempotencyStore.tryAcquire(anyString(), anyString())).thenReturn(false);

        listener.onUserCreated(event);

        verify(passengerService, never()).register(any(), anyString());
        verify(eventMapper, never()).toCommand(any());
    }

    @Test
    void onUserCreated_shouldPropagateExceptionOnRegisterFailure() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "fail@example.com", "PASSENGER", List.of("standard"));

        CreatePassengerCommand cmd = new CreatePassengerCommand(externalId, "fail@example.com", "STANDARD");
        when(eventMapper.toCommand(any(UserCreatedEventDTO.class))).thenReturn(cmd);
        doThrow(new RuntimeException("DB down")).when(passengerService).register(eq(cmd), anyString());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
            () -> createListener().onUserCreated(event));
    }
}
