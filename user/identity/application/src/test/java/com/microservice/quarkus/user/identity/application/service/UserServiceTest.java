package com.microservice.quarkus.user.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.api.KeycloakProvider;
import com.microservice.quarkus.user.identity.application.dto.CreateUserCommand;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.shared.application.outbox.EventMetadata;
import com.microservice.quarkus.user.shared.application.outbox.TraceContextProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    KeycloakProvider keycloakClient;

    @Mock
    IdentitySyncRepository syncRepository;

    @Mock
    IdentityPersistenceService persistenceService;

    @Mock
    TraceContextProvider traceContextProvider;

    private UserService createService() {
        lenient().when(traceContextProvider.current()).thenReturn(EventMetadata.empty());
        return new UserService(keycloakClient, syncRepository, persistenceService, traceContextProvider);
    }

    @Test
    void register_shouldCreateNewUserWhenNotExists() {
        when(syncRepository.findByEmail("new@example.com")).thenReturn(null);
        when(keycloakClient.createUser("new@example.com", "pass123")).thenReturn("kc-123");

        CreateUserCommand cmd = new CreateUserCommand("new@example.com", null, "pass123", "PASSENGER", Set.of("basic"), false);
        String result = createService().register(cmd);

        assertEquals("kc-123", result);
        verify(persistenceService).persistPendingSync(
            eq("new@example.com"), eq("PASSENGER"), eq(List.of("basic")), eq("kc-123"),
            any(), any(), any(), any());
    }

    @Test
    void register_shouldReturnExistingExternalIdWhenAlreadySynced() {
        UserSyncRecord existing = new UserSyncRecord("id-1", "existing@example.com", "ext-123",
            "PASSENGER", List.of("basic"), SyncStatus.SYNCED, null, null);
        when(syncRepository.findByEmail("existing@example.com")).thenReturn(existing);

        CreateUserCommand cmd = new CreateUserCommand("existing@example.com", null, "pass", "PASSENGER", Set.of("basic"), false);
        String result = createService().register(cmd);

        assertEquals("ext-123", result);
        verify(keycloakClient, never()).createUser(anyString(), anyString());
        verify(persistenceService, never()).persistSyncAndOutbox(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void register_shouldHandleKeycloakFailure() {
        when(syncRepository.findByEmail("fail@example.com")).thenReturn(null);
        when(keycloakClient.createUser(eq("fail@example.com"), anyString()))
                .thenThrow(new RuntimeException("Keycloak unavailable"));

        CreateUserCommand cmd = new CreateUserCommand("fail@example.com", null, "pass", "PASSENGER", Set.of("basic"), false);
        String result = createService().register(cmd);

        assertNull(result);
        verify(persistenceService).persistFailedSync("fail@example.com", "PASSENGER", List.of("basic"));
        verify(persistenceService, never()).persistSyncAndOutbox(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void register_shouldCreateUserCommandFromWebhook() {
        CreateUserCommand cmd = CreateUserCommand.fromWebhook(
                "user@example.com", "ext-123", "ADMIN", Set.of("editor"));

        assertEquals("user@example.com", cmd.email());
        assertEquals("ext-123", cmd.externalId());
        assertEquals("ADMIN", cmd.userType());
        assertEquals(Set.of("editor"), cmd.roles());
        assertTrue(cmd.isFromKeycloak());
    }

    @Test
    void register_shouldCreateUserCommandFromWebhook2() {
        CreateUserCommand cmd = CreateUserCommand.fromWebhook2(
                "user2@example.com", "password123", "PASSENGER", Set.of("basic"));

        assertEquals("user2@example.com", cmd.email());
        assertEquals("password123", cmd.password());
        assertEquals("PASSENGER", cmd.userType());
        assertEquals(Set.of("basic"), cmd.roles());
        assertFalse(cmd.isFromKeycloak());
    }

    @Test
    void register_shouldUseExternalIdWhenFromKeycloak() {
        when(syncRepository.findByEmail("webhook@example.com")).thenReturn(null);

        CreateUserCommand cmd = CreateUserCommand.fromWebhook("webhook@example.com", "kc-webhook-123", "PASSENGER", Set.of("basic"));
        String result = createService().register(cmd);

        assertEquals("kc-webhook-123", result);
        verify(keycloakClient, never()).createUser(anyString(), anyString());
        verify(persistenceService).persistSyncAndOutbox(
            eq("webhook@example.com"), eq("PASSENGER"), eq(List.of("basic")), eq("kc-webhook-123"),
            any(), any(), any(), any(), any());
    }

    @Test
    void register_shouldNotCompensateWhenDbFails() {
        when(syncRepository.findByEmail("dbfail@example.com")).thenReturn(null);
        when(keycloakClient.createUser("dbfail@example.com", "pass123")).thenReturn("kc-to-delete");

        doThrow(new RuntimeException("DB connection lost"))
            .when(persistenceService).persistPendingSync(any(), any(), any(), any(), any(), any(), any(), any());

        CreateUserCommand cmd = new CreateUserCommand("dbfail@example.com", null, "pass123", "PASSENGER", Set.of("basic"), false);

        assertThrows(RuntimeException.class, () -> createService().register(cmd));
        verify(keycloakClient, never()).deleteUser(anyString());
        verify(persistenceService, never()).persistFailedSync(any(), any(), any());
    }

    @Test
    void register_shouldSkipPendingRecordsAndReturnExternalId() {
        UserSyncRecord pendingRecord = new UserSyncRecord("id-2", "pending@example.com", "ext-pending",
            "PASSENGER", List.of("basic"), SyncStatus.PENDING, null, null);
        when(syncRepository.findByEmail("pending@example.com")).thenReturn(pendingRecord);

        CreateUserCommand cmd = new CreateUserCommand("pending@example.com", null, "pass", "PASSENGER", Set.of("basic"), false);
        String result = createService().register(cmd);

        assertEquals("ext-pending", result);
        verify(keycloakClient, never()).createUser(anyString(), anyString());
        verify(persistenceService, never()).persistPendingSync(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void register_shouldReturnExistingExternalIdWhenAlreadyPending() {
        UserSyncRecord pendingRecord = new UserSyncRecord("id-3", "pending2@example.com", "ext-pending2",
            "PASSENGER", List.of("basic"), SyncStatus.PENDING, null, null);
        when(syncRepository.findByEmail("pending2@example.com")).thenReturn(pendingRecord);

        CreateUserCommand cmd = new CreateUserCommand("pending2@example.com", null, "pass", "PASSENGER", Set.of("basic"), false);
        String result = createService().register(cmd);

        assertEquals("ext-pending2", result);
        verify(keycloakClient, never()).createUser(anyString(), anyString());
        verify(persistenceService, never()).persistPendingSync(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteByExternalId_shouldDelegateToPersistenceService() {
        createService().deleteByExternalId("kc-user-123", "corr-delete-1");

        verify(persistenceService).deleteSyncRecord("kc-user-123", "corr-delete-1");
    }
}
