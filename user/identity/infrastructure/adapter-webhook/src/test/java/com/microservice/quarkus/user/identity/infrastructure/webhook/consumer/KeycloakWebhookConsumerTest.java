package com.microservice.quarkus.user.identity.infrastructure.webhook.consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.api.KeycloakProvider;
import com.microservice.quarkus.user.identity.application.dto.KeycloakUserDTO;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.dto.UserType;
import com.microservice.quarkus.user.identity.application.service.UserService;
import com.microservice.quarkus.user.identity.infrastructure.webhook.event.WebhookKeycloakPayload;
import com.microservice.quarkus.user.shared.infrastructure.eventbus.CorrelationIdInterceptor;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class KeycloakWebhookConsumerTest {

    @Mock
    ClientIdentityProvider clientService;

    @Mock
    KeycloakProvider keycloakProvider;

    @Mock
    UserService userService;

    @Mock
    CorrelationIdInterceptor correlationIdInterceptor;

    @Mock
    IdentitySyncRepository syncRepository;

    @Mock
    Tracer tracer;

    @BeforeEach
    void setUp() {
        Span span = mock(Span.class);
        SpanBuilder spanBuilder = mock(SpanBuilder.class);
        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setParent(any())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.makeCurrent()).thenReturn(mock(Scope.class));
    }

    private KeycloakWebhookConsumer createConsumer() {
        KeycloakWebhookConsumer consumer = new KeycloakWebhookConsumer(clientService, keycloakProvider, userService, correlationIdInterceptor, syncRepository);
        try {
            Field tracerField = KeycloakWebhookConsumer.class.getDeclaredField("tracer");
            tracerField.setAccessible(true);
            tracerField.set(consumer, tracer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return consumer;
    }

    @Test
    void processKeycloakEvent_shouldRegisterUserOnRegisterEventType() {
        String userId = UUID.randomUUID().toString();
        String clientId = UUID.randomUUID().toString();

        when(syncRepository.findByExternalId(userId)).thenReturn(null);
        when(syncRepository.findByEmail(userId)).thenReturn(null);

        KeycloakUserDTO keycloakUser = new KeycloakUserDTO(userId, "user@example.com", userId, "PASSENGER");
        when(keycloakProvider.getUserById(userId)).thenReturn(keycloakUser);
        when(clientService.getClientNameById(clientId)).thenReturn("frontend-client");
        when(correlationIdInterceptor.getOrCreateCorrelationId()).thenReturn("corr-123");
        when(userService.register(any(), eq("corr-123"))).thenReturn(userId);

        WebhookKeycloakPayload payload = new WebhookKeycloakPayload("REGISTER", userId, clientId, Map.of(), null, null);
        createConsumer().processKeycloakEvent(payload);

        verify(keycloakProvider).getUserById(userId);
        verify(userService).register(argThat(cmd ->
            cmd.email().equals("user@example.com") &&
            cmd.externalId().equals(userId) &&
            cmd.isFromKeycloak() &&
            "PASSENGER".equals(cmd.type())
        ), eq("corr-123"));
    }

    @Test
    void processKeycloakEvent_shouldMapDashboardClientToAdmin() {
        String userId = UUID.randomUUID().toString();
        String clientId = UUID.randomUUID().toString();

        when(syncRepository.findByExternalId(userId)).thenReturn(null);
        when(syncRepository.findByEmail(userId)).thenReturn(null);

        KeycloakUserDTO keycloakUser = new KeycloakUserDTO(userId, "admin@example.com", userId, "ADMIN");
        when(keycloakProvider.getUserById(userId)).thenReturn(keycloakUser);
        when(clientService.getClientNameById(clientId)).thenReturn("dashboard-client");
        when(correlationIdInterceptor.getOrCreateCorrelationId()).thenReturn("corr-456");
        when(userService.register(any(), eq("corr-456"))).thenReturn(userId);

        WebhookKeycloakPayload payload = new WebhookKeycloakPayload("REGISTER", userId, clientId, Map.of(), null, null);
        createConsumer().processKeycloakEvent(payload);

        verify(userService).register(argThat(cmd ->
            "ADMIN".equals(cmd.type())
        ), eq("corr-456"));
    }

    @Test
    void processKeycloakEvent_shouldNotRegisterOrDeleteUserOnNonRegisterDeleteEventType() {
        String userId = UUID.randomUUID().toString();
        String clientId = UUID.randomUUID().toString();

        WebhookKeycloakPayload payload = new WebhookKeycloakPayload("LOGIN", userId, clientId, Map.of(), null, null);
        createConsumer().processKeycloakEvent(payload);

        verify(keycloakProvider, never()).getUserById(anyString());
        verify(userService, never()).register(any(), anyString());
        verify(userService, never()).deleteByExternalId(anyString(), anyString());
    }

    @Test
    void processKeycloakEvent_shouldDeleteSyncRecordOnDeleteEventType() {
        String userId = UUID.randomUUID().toString();
        String clientId = UUID.randomUUID().toString();

        when(correlationIdInterceptor.getOrCreateCorrelationId()).thenReturn("corr-delete-1");

        WebhookKeycloakPayload payload = new WebhookKeycloakPayload("DELETE", userId, clientId, Map.of(), null, null);
        createConsumer().processKeycloakEvent(payload);

        verify(keycloakProvider, never()).getUserById(anyString());
        verify(userService).deleteByExternalId(userId, "corr-delete-1");
    }

    @Test
    void processKeycloakEvent_shouldPropagateExceptionOnDeleteFailure() {
        String userId = UUID.randomUUID().toString();
        String clientId = UUID.randomUUID().toString();

        when(correlationIdInterceptor.getOrCreateCorrelationId()).thenReturn("corr-delete-2");
        doThrow(new RuntimeException("DB down")).when(userService).deleteByExternalId(anyString(), anyString());

        WebhookKeycloakPayload payload = new WebhookKeycloakPayload("DELETE", userId, clientId, Map.of(), null, null);

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> createConsumer().processKeycloakEvent(payload));

        assertEquals("DB down", thrown.getMessage());
    }

    @Test
    void processKeycloakEvent_shouldPropagateExceptionOnFailure() {
        String userId = UUID.randomUUID().toString();
        String clientId = UUID.randomUUID().toString();

        when(syncRepository.findByExternalId(userId)).thenReturn(null);
        when(syncRepository.findByEmail(userId)).thenReturn(null);

        KeycloakUserDTO keycloakUser = new KeycloakUserDTO(userId, "fail@example.com", userId, "PASSENGER");
        when(keycloakProvider.getUserById(userId)).thenReturn(keycloakUser);
        when(clientService.getClientNameById(clientId)).thenReturn("frontend-client");
        when(correlationIdInterceptor.getOrCreateCorrelationId()).thenReturn("corr-789");
        when(userService.register(any(), anyString())).thenThrow(new RuntimeException("DB down"));

        WebhookKeycloakPayload payload = new WebhookKeycloakPayload("REGISTER", userId, clientId, Map.of(), null, null);

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> createConsumer().processKeycloakEvent(payload));

        assertEquals("DB down", thrown.getMessage());
    }

    @Test
    void processKeycloakEvent_shouldSkipAlreadySyncedUser() {
        String userId = UUID.randomUUID().toString();
        String clientId = UUID.randomUUID().toString();

        UserSyncRecord syncedRecord = new UserSyncRecord(
            UUID.randomUUID().toString(),
            "user@example.com",
            userId,
            UserType.PASSENGER,
            SyncStatus.SYNCED,
            null,
            null
        );
        when(syncRepository.findByExternalId(userId)).thenReturn(syncedRecord);

        WebhookKeycloakPayload payload = new WebhookKeycloakPayload("REGISTER", userId, clientId, Map.of(), null, null);
        createConsumer().processKeycloakEvent(payload);

        verify(syncRepository).findByExternalId(userId);
        verify(userService, never()).register(any(), anyString());
    }

    @Test
    void processKeycloakEvent_shouldSkipPendingUser() {
        String userId = UUID.randomUUID().toString();
        String clientId = UUID.randomUUID().toString();

        UserSyncRecord pendingRecord = new UserSyncRecord(
            UUID.randomUUID().toString(),
            "user@example.com",
            userId,
            UserType.PASSENGER,
            SyncStatus.PENDING,
            null,
            null
        );
        when(syncRepository.findByExternalId(userId)).thenReturn(pendingRecord);

        WebhookKeycloakPayload payload = new WebhookKeycloakPayload("REGISTER", userId, clientId, Map.of(), null, null);
        createConsumer().processKeycloakEvent(payload);

        verify(syncRepository).findByExternalId(userId);
        verify(userService, never()).register(any(), anyString());
    }
}
