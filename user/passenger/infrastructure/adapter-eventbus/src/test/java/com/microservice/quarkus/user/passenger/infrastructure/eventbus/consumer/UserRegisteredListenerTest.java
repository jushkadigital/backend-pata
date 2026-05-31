package com.microservice.quarkus.user.passenger.infrastructure.eventbus.consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.microservice.quarkus.user.passenger.application.api.PassengerRepository;
import com.microservice.quarkus.user.passenger.application.dto.CreatePassengerCommand;
import com.microservice.quarkus.user.passenger.application.service.PassengerService;
import com.microservice.quarkus.user.passenger.domain.Passenger;
import com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl.UserRegisteredEventDTO;
import com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl.UserRegisteredEventMapper;
import com.microservice.quarkus.user.identity.application.api.GroupIdentityProvider;

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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegisteredListenerTest {

    @Mock
    PassengerService passengerService;

    @Mock
    PassengerRepository passengerRepository;

    @Mock
    UserRegisteredEventMapper eventMapper;

    @Mock
    GroupIdentityProvider groupIdentityProvider;

    @Mock
    Tracer tracer;

    @Mock
    SpanBuilder spanBuilder;

    @Mock
    Span span;

    @Mock
    Scope scope;

    private UserRegisteredListener createListener() {
        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setParent(any())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.makeCurrent()).thenReturn(scope);
        UserRegisteredListener listener = new UserRegisteredListener(passengerService, passengerRepository, eventMapper, groupIdentityProvider);
        listener.tracer = tracer;
        return listener;
    }

    private String buildEventJson(String aggregateId, String email, String type, String subType) {
        UserRegisteredEventDTO dto = new UserRegisteredEventDTO(
            aggregateId, "User", "identity.user.registered.v1", 1,
            email, type, subType,
            "corr-" + UUID.randomUUID(), null, "trace-1", "span-1",
            "user-service", null, null,
            UUID.randomUUID(), Instant.now());
        return JsonObject.mapFrom(dto).encode();
    }

    @Test
    void onUserRegistered_shouldCreatePassengerForPassengerTypeEvent() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "user@example.com", "PASSENGER", "STANDARD");

        when(passengerRepository.findByExternalId(externalId)).thenReturn(Optional.empty());
        CreatePassengerCommand cmd = new CreatePassengerCommand(externalId, "user@example.com", "PASSENGER", "STANDARD");
        when(eventMapper.toCommand(any(UserRegisteredEventDTO.class))).thenReturn(cmd);
        when(groupIdentityProvider.findGroupByPath("passenger/standard")).thenReturn("group-id-1");

        createListener().onUserRegistered(event);

        verify(passengerService).register(eq(cmd), anyString());
        verify(groupIdentityProvider).assignUserToGroup(eq(externalId), eq("group-id-1"));
    }

    @Test
    void onUserRegistered_shouldIgnoreNonPassengerTypeEvent() {
        String event = buildEventJson(UUID.randomUUID().toString(), "admin@example.com", "ADMIN", "STANDARD");

        createListener().onUserRegistered(event);

        verify(passengerService, never()).register(any(), anyString());
        verify(eventMapper, never()).toCommand(any());
        verify(groupIdentityProvider, never()).assignUserToGroup(anyString(), anyString());
    }

    @Test
    void onUserRegistered_shouldSkipIdempotentlyIfPassengerAlreadyExists() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "existing@example.com", "PASSENGER", "STANDARD");

        when(passengerRepository.findByExternalId(externalId))
            .thenReturn(Optional.of(mock(Passenger.class)));
        when(groupIdentityProvider.findGroupByPath("passenger/standard")).thenReturn("group-id-1");

        createListener().onUserRegistered(event);

        verify(passengerService, never()).register(any(), anyString());
        verify(eventMapper, never()).toCommand(any());
        // Group assignment should still happen even when passenger already existed
        verify(groupIdentityProvider).assignUserToGroup(eq(externalId), eq("group-id-1"));
    }

    @Test
    void onUserRegistered_shouldSkipGroupAssignmentForUnknownSubType() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "user@example.com", "PASSENGER", "UNKNOWN_TYPE");

        when(passengerRepository.findByExternalId(externalId)).thenReturn(Optional.empty());
        CreatePassengerCommand cmd = new CreatePassengerCommand(externalId, "user@example.com", "PASSENGER", "UNKNOWN_TYPE");
        when(eventMapper.toCommand(any(UserRegisteredEventDTO.class))).thenReturn(cmd);

        createListener().onUserRegistered(event);

        verify(groupIdentityProvider, never()).findGroupByPath(anyString());
        verify(groupIdentityProvider, never()).assignUserToGroup(anyString(), anyString());
    }

    @Test
    void onUserRegistered_shouldSkipGroupAssignmentWhenGroupNotFound() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "user@example.com", "PASSENGER", "BASIC");

        when(passengerRepository.findByExternalId(externalId)).thenReturn(Optional.empty());
        CreatePassengerCommand cmd = new CreatePassengerCommand(externalId, "user@example.com", "PASSENGER", "BASIC");
        when(eventMapper.toCommand(any(UserRegisteredEventDTO.class))).thenReturn(cmd);
        when(groupIdentityProvider.findGroupByPath("passenger/basic")).thenReturn(null);

        createListener().onUserRegistered(event);

        verify(groupIdentityProvider, never()).assignUserToGroup(anyString(), anyString());
    }

    @Test
    void onUserRegistered_shouldPropagateExceptionOnRegisterFailure() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "fail@example.com", "PASSENGER", "STANDARD");

        when(passengerRepository.findByExternalId(externalId)).thenReturn(Optional.empty());
        CreatePassengerCommand cmd = new CreatePassengerCommand(externalId, "fail@example.com", "PASSENGER", "STANDARD");
        when(eventMapper.toCommand(any(UserRegisteredEventDTO.class))).thenReturn(cmd);
        doThrow(new RuntimeException("DB down")).when(passengerService).register(eq(cmd), anyString());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
            () -> createListener().onUserRegistered(event));
    }
}
