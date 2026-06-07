package com.microservice.quarkus.user.identity.infrastructure.eventbus.consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.api.KeycloakProvider;
import com.microservice.quarkus.user.identity.application.dto.KeycloakUserDTO;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakReconciliationJobTest {

    @Mock
    KeycloakProvider keycloakProvider;

    @Mock
    IdentitySyncRepository syncRepository;

    @Mock
    MeterRegistry meterRegistry;

    @Mock
    Counter reconciledCounter;

    private KeycloakReconciliationJob job;

    @BeforeEach
    void setUp() {
        job = new KeycloakReconciliationJob();
        job.keycloakProvider = keycloakProvider;
        job.syncRepository = syncRepository;
        job.meterRegistry = meterRegistry;

        // Initialize the counter using reflection
        try {
            Field counterField = KeycloakReconciliationJob.class.getDeclaredField("reconciledCounter");
            counterField.setAccessible(true);
            counterField.set(job, reconciledCounter);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize reconciledCounter", e);
        }
    }

@Test
    void reconcile_shouldCreatePendingRecordForOrphanUser() {
        // Arrange
        String externalId = "ext-123";
        String email = "orphan@example.com";
        KeycloakUserDTO kcUser = new KeycloakUserDTO(externalId, email, externalId, "PASSENGER");

        when(keycloakProvider.getAllUsers()).thenReturn(List.of(kcUser));
        when(syncRepository.findByExternalId(externalId)).thenReturn(null);
        when(syncRepository.findByEmail(email)).thenReturn(null);

        // Act
        job.reconcile();

// Assert
        verify(syncRepository).save(argThat(record ->
            record.email().equals(email) &&
            record.externalId().equals(externalId) &&
            record.userType().equals("PASSENGER") &&
            record.roles().equals(List.of("basic")) &&
            record.syncStatus() == SyncStatus.PENDING
        ));
        verify(reconciledCounter).increment(1.0d);
    }

    @Test
    void reconcile_shouldNotCreateRecordForExistingSyncedUser() {
        // Arrange
        String externalId = "ext-123";
        String email = "existing@example.com";
        KeycloakUserDTO kcUser = new KeycloakUserDTO(externalId, email, externalId, "PASSENGER");

        UserSyncRecord existingRecord = new UserSyncRecord(
            "id-1", email, externalId, "PASSENGER", List.of("basic"), SyncStatus.SYNCED,
            Instant.now(), Instant.now()
        );

        when(keycloakProvider.getAllUsers()).thenReturn(List.of(kcUser));
        when(syncRepository.findByExternalId(externalId)).thenReturn(existingRecord);

        // Act
        job.reconcile();

        // Assert
        verify(syncRepository, never()).save(any(UserSyncRecord.class));
        verify(reconciledCounter, never()).increment();
    }

    @Test
    void reconcile_shouldUpdatePendingRecordWithExternalId() {
        // Arrange
        String externalId = "ext-123";
        String email = "pending@example.com";
        KeycloakUserDTO kcUser = new KeycloakUserDTO(externalId, email, externalId, "PASSENGER");

        UserSyncRecord pendingRecord = new UserSyncRecord(
            "id-2", email, null, "PASSENGER", List.of("basic"), SyncStatus.PENDING,
            Instant.now(), Instant.now()
        );

        when(keycloakProvider.getAllUsers()).thenReturn(List.of(kcUser));
        when(syncRepository.findByExternalId(externalId)).thenReturn(null);
        when(syncRepository.findByEmail(email)).thenReturn(pendingRecord);

        // Act
        job.reconcile();

        // Assert
        verify(syncRepository).save(argThat(record ->
            record.email().equals(email) &&
            record.externalId().equals(externalId) &&
            record.syncStatus() == SyncStatus.PENDING
        ));
        verify(reconciledCounter, never()).increment();
    }

    @Test
    void reconcile_shouldDoNothingWhenNoOrphans() {
        // Arrange
        String externalId = "ext-123";
        String email = "existing@example.com";
        KeycloakUserDTO kcUser = new KeycloakUserDTO(externalId, email, externalId, "PASSENGER");

        UserSyncRecord existingRecord = new UserSyncRecord(
            "id-1", email, externalId, "PASSENGER", List.of("basic"), SyncStatus.SYNCED,
            Instant.now(), Instant.now()
        );

        when(keycloakProvider.getAllUsers()).thenReturn(List.of(kcUser));
        when(syncRepository.findByExternalId(externalId)).thenReturn(existingRecord);

        // Act
        job.reconcile();

        // Assert
        verify(syncRepository, never()).save(any(UserSyncRecord.class));
        verify(reconciledCounter, never()).increment();
    }

    @Test
    void reconcile_shouldHandleKeycloakApiFailure() {
        // Arrange
        when(keycloakProvider.getAllUsers()).thenThrow(new RuntimeException("Keycloak API unavailable"));

        // Act
        job.reconcile();

        // Assert
        verify(syncRepository, never()).save(any(UserSyncRecord.class));
        verify(reconciledCounter, never()).increment();
    }

    @Test
    void reconcile_shouldHandleEmptyKeycloakUsers() {
        // Arrange
        when(keycloakProvider.getAllUsers()).thenReturn(List.of());

        // Act
        job.reconcile();

        // Assert
        verify(syncRepository, never()).save(any(UserSyncRecord.class));
        verify(reconciledCounter, never()).increment();
    }

    @Test
    void reconcile_shouldDefaultToPassengerType() {
        // Arrange
        String externalId = "ext-123";
        String email = "orphan@example.com";
        KeycloakUserDTO kcUser = new KeycloakUserDTO(externalId, email, externalId, "PASSENGER");

        when(keycloakProvider.getAllUsers()).thenReturn(List.of(kcUser));
        when(syncRepository.findByExternalId(externalId)).thenReturn(null);
        when(syncRepository.findByEmail(email)).thenReturn(null);

        // Act
        job.reconcile();

// Assert
        verify(syncRepository).save(argThat(record ->
            record.email().equals(email) &&
            record.externalId().equals(externalId) &&
            record.userType().equals("PASSENGER") &&
            record.roles().equals(List.of("basic")) &&
            record.syncStatus() == SyncStatus.PENDING
        ));
        verify(reconciledCounter).increment(1.0d);
    }

    @Test
    void reconcile_shouldHandleMultipleOrphans() {
        // Arrange
        KeycloakUserDTO kcUser1 = new KeycloakUserDTO("ext-1", "orphan1@example.com", "ext-1", "PASSENGER");
        KeycloakUserDTO kcUser2 = new KeycloakUserDTO("ext-2", "orphan2@example.com", "ext-2", "PASSENGER");
        KeycloakUserDTO kcUser3 = new KeycloakUserDTO("ext-3", "orphan3@example.com", "ext-3", "PASSENGER");

        when(keycloakProvider.getAllUsers()).thenReturn(List.of(kcUser1, kcUser2, kcUser3));
        when(syncRepository.findByExternalId(anyString())).thenReturn(null);
        when(syncRepository.findByEmail(anyString())).thenReturn(null);

        // Act
        job.reconcile();

        // Assert
        verify(syncRepository, times(3)).save(any(UserSyncRecord.class));
        verify(reconciledCounter).increment(3.0d);
    }

    @Test
    void reconcile_shouldHandleMixedOrphansAndExisting() {
        // Arrange
        KeycloakUserDTO kcUser1 = new KeycloakUserDTO("ext-1", "orphan1@example.com", "ext-1", "PASSENGER");
        KeycloakUserDTO kcUser2 = new KeycloakUserDTO("ext-2", "existing@example.com", "ext-2", "PASSENGER");

        UserSyncRecord existingRecord = new UserSyncRecord(
            "id-2", "existing@example.com", "ext-2", "PASSENGER", List.of("basic"), SyncStatus.SYNCED,
            Instant.now(), Instant.now()
        );

        when(keycloakProvider.getAllUsers()).thenReturn(List.of(kcUser1, kcUser2));
        when(syncRepository.findByExternalId("ext-1")).thenReturn(null);
        when(syncRepository.findByEmail("orphan1@example.com")).thenReturn(null);
        when(syncRepository.findByExternalId("ext-2")).thenReturn(existingRecord);

        // Act
        job.reconcile();

        // Assert
        verify(syncRepository, times(1)).save(any(UserSyncRecord.class));
        verify(reconciledCounter).increment(1.0d);
    }
}
