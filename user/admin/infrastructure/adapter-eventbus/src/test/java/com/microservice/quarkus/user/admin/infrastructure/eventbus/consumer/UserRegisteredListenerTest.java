package com.microservice.quarkus.user.admin.infrastructure.eventbus.consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.microservice.quarkus.user.admin.application.dto.CreateAdminCommand;
import com.microservice.quarkus.user.admin.application.service.AdminService;
import com.microservice.quarkus.user.admin.infrastructure.eventbus.acl.UserRegisteredEventDTO;
import com.microservice.quarkus.user.admin.infrastructure.eventbus.acl.UserRegisteredEventMapper;
import com.microservice.quarkus.user.identity.application.api.GroupIdentityProvider;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.vertx.core.json.JsonObject;
import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegisteredListenerTest {

    @Mock
    AdminService adminService;

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
        UserRegisteredListener listener = new UserRegisteredListener(adminService, eventMapper, groupIdentityProvider);
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
    void onUserRegistered_shouldCreateAdminForAdminTypeEvent() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "admin@example.com", "ADMIN", "SUPER_ADMIN");

        CreateAdminCommand cmd = new CreateAdminCommand(externalId, "admin@example.com", "SUPER_ADMIN");
        when(eventMapper.toCommand(any(UserRegisteredEventDTO.class))).thenReturn(cmd);
        when(adminService.register(eq(cmd), anyString())).thenReturn(externalId);
        when(groupIdentityProvider.findGroupByPath("admin/super-admin")).thenReturn("group-id-1");

        createListener().onUserRegistered(event);

        verify(adminService).register(eq(cmd), anyString());
        verify(groupIdentityProvider).assignUserToGroup(eq(externalId), eq("group-id-1"));
    }

    @Test
    void onUserRegistered_shouldIgnoreNonAdminTypeEvent() {
        String event = buildEventJson(UUID.randomUUID().toString(), "user@example.com", "PASSENGER", "STANDARD");

        createListener().onUserRegistered(event);

        verify(adminService, never()).register(any(), anyString());
        verify(eventMapper, never()).toCommand(any());
        verify(groupIdentityProvider, never()).assignUserToGroup(anyString(), anyString());
    }

    @Test
    void onUserRegistered_shouldHandleIdempotentEntityExistsException() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "existing@example.com", "ADMIN", "ADMIN");

        CreateAdminCommand cmd = new CreateAdminCommand(externalId, "existing@example.com", "ADMIN");
        when(eventMapper.toCommand(any(UserRegisteredEventDTO.class))).thenReturn(cmd);
        doThrow(new EntityExistsException("Already exists")).when(adminService).register(eq(cmd), anyString());
        when(groupIdentityProvider.findGroupByPath("admin/admin")).thenReturn("group-id-1");

        createListener().onUserRegistered(event);

        verify(adminService).register(eq(cmd), anyString());
        // Group assignment should still happen even when admin already existed
        verify(groupIdentityProvider).assignUserToGroup(eq(externalId), eq("group-id-1"));
    }

    @Test
    void onUserRegistered_shouldSkipGroupAssignmentForUnknownSubType() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "admin@example.com", "ADMIN", "UNKNOWN_TYPE");

        CreateAdminCommand cmd = new CreateAdminCommand(externalId, "admin@example.com", "UNKNOWN_TYPE");
        when(eventMapper.toCommand(any(UserRegisteredEventDTO.class))).thenReturn(cmd);
        when(adminService.register(eq(cmd), anyString())).thenReturn(externalId);

        createListener().onUserRegistered(event);

        verify(groupIdentityProvider, never()).findGroupByPath(anyString());
        verify(groupIdentityProvider, never()).assignUserToGroup(anyString(), anyString());
    }

    @Test
    void onUserRegistered_shouldSkipGroupAssignmentWhenGroupNotFound() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "admin@example.com", "ADMIN", "EDITOR");

        CreateAdminCommand cmd = new CreateAdminCommand(externalId, "admin@example.com", "EDITOR");
        when(eventMapper.toCommand(any(UserRegisteredEventDTO.class))).thenReturn(cmd);
        when(adminService.register(eq(cmd), anyString())).thenReturn(externalId);
        when(groupIdentityProvider.findGroupByPath("admin/editor")).thenReturn(null);

        createListener().onUserRegistered(event);

        verify(groupIdentityProvider, never()).assignUserToGroup(anyString(), anyString());
    }

    @Test
    void onUserRegistered_shouldPropagateNonIdempotentException() {
        String externalId = UUID.randomUUID().toString();
        String event = buildEventJson(externalId, "fail@example.com", "ADMIN", "SUPER_ADMIN");

        CreateAdminCommand cmd = new CreateAdminCommand(externalId, "fail@example.com", "SUPER_ADMIN");
        when(eventMapper.toCommand(any(UserRegisteredEventDTO.class))).thenReturn(cmd);
        doThrow(new RuntimeException("DB down")).when(adminService).register(eq(cmd), anyString());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
            () -> createListener().onUserRegistered(event));
    }
}
