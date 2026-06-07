package com.microservice.quarkus.user.identity.infrastructure.eventbus.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.service.IdentityPersistenceService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SyncStatusProcessorTest {

    @Mock
    private IdentitySyncRepository syncRepository;

    @Mock
    private IdentityPersistenceService persistenceService;

    @Mock
    private MeterRegistry meterRegistry;

    private SyncStatusProcessor processor;
    private Counter syncedCounter;
    private Counter failedCounter;

    @BeforeEach
    void setUp() throws Exception {
        processor = new SyncStatusProcessor();
        syncedCounter = mock(Counter.class);
        failedCounter = mock(Counter.class);

        // Simulate onStart() method
        lenient().when(meterRegistry.counter(anyString())).thenReturn(syncedCounter, failedCounter);

        // Use reflection to set private fields
        Field syncedCounterField = SyncStatusProcessor.class.getDeclaredField("syncedCounter");
        syncedCounterField.setAccessible(true);
        syncedCounterField.set(processor, syncedCounter);

        Field failedCounterField = SyncStatusProcessor.class.getDeclaredField("failedCounter");
        failedCounterField.setAccessible(true);
        failedCounterField.set(processor, failedCounter);

        Field syncRepositoryField = SyncStatusProcessor.class.getDeclaredField("syncRepository");
        syncRepositoryField.setAccessible(true);
        syncRepositoryField.set(processor, syncRepository);

        Field persistenceServiceField = SyncStatusProcessor.class.getDeclaredField("persistenceService");
        persistenceServiceField.setAccessible(true);
        persistenceServiceField.set(processor, persistenceService);
    }

    @Test
    void processPendingSyncs_shouldCompletePendingRecord() {
        // Given
        UserSyncRecord readyRecord = new UserSyncRecord(
            "id-1", "test@example.com", "ext-1", "PASSENGER", List.of("basic"),
            SyncStatus.PENDING, Instant.now().minusSeconds(60), Instant.now(), 0, 5, null
        );

        when(syncRepository.findBySyncStatus(SyncStatus.PENDING)).thenReturn(List.of(readyRecord));

        // When
        processor.processPendingSyncs();

        // Then
        verify(syncRepository).findBySyncStatus(SyncStatus.PENDING);
        verify(persistenceService).completeSync(readyRecord);
        verify(syncedCounter).increment();
        verify(syncRepository, never()).save(any());
        verify(failedCounter, never()).increment();
    }

    @Test
    void processPendingSyncs_shouldSkipNotReadyRecord() {
        // Given
        UserSyncRecord notReadyRecord = new UserSyncRecord(
            "id-2", "future@example.com", "ext-2", "PASSENGER", List.of("basic"),
            SyncStatus.PENDING, Instant.now().minusSeconds(60), Instant.now(), 0, 5,
            Instant.now().plusSeconds(300)
        );

        when(syncRepository.findBySyncStatus(SyncStatus.PENDING)).thenReturn(List.of(notReadyRecord));

        // When
        processor.processPendingSyncs();

        // Then
        verify(syncRepository).findBySyncStatus(SyncStatus.PENDING);
        verify(persistenceService, never()).completeSync(any());
        verify(syncedCounter, never()).increment();
        verify(syncRepository, never()).save(any());
        verify(failedCounter, never()).increment();
    }

    @Test
    void processPendingSyncs_shouldRetryOnFailure() {
        // Given
        UserSyncRecord record = mock(UserSyncRecord.class);
        UserSyncRecord updatedRecord = new UserSyncRecord(
            "id-3", "retry@example.com", "ext-3", "PASSENGER", List.of("basic"),
            SyncStatus.PENDING, Instant.now().minusSeconds(60), Instant.now(), 3, 5,
            Instant.now().plusSeconds(120)
        );

        when(syncRepository.findBySyncStatus(SyncStatus.PENDING)).thenReturn(List.of(record));
        doThrow(new RuntimeException("Sync failed")).when(persistenceService).completeSync(record);
        when(record.isReady()).thenReturn(true);
        when(record.incrementRetry()).thenReturn(updatedRecord);

        // When
        processor.processPendingSyncs();

        // Then
        verify(syncRepository).findBySyncStatus(SyncStatus.PENDING);
        verify(persistenceService).completeSync(record);
        verify(syncedCounter, never()).increment();
        verify(failedCounter, never()).increment();
        verify(syncRepository).save(updatedRecord);
    }

    @Test
    void processPendingSyncs_shouldMarkFailedAfterMaxRetries() {
        // Given
        UserSyncRecord record = mock(UserSyncRecord.class);
        UserSyncRecord failedRecord = mock(UserSyncRecord.class);
        UserSyncRecord failedWithStatus = mock(UserSyncRecord.class);

        when(syncRepository.findBySyncStatus(SyncStatus.PENDING)).thenReturn(List.of(record));
        doThrow(new RuntimeException("Max retries exceeded")).when(persistenceService).completeSync(record);
        when(record.isReady()).thenReturn(true);
        when(record.incrementRetry()).thenReturn(failedRecord);
        when(failedRecord.exceededMaxRetries()).thenReturn(true);
        when(failedRecord.withSyncStatus(SyncStatus.FAILED)).thenReturn(failedWithStatus);

        // When
        processor.processPendingSyncs();

        // Then
        verify(syncRepository).findBySyncStatus(SyncStatus.PENDING);
        verify(persistenceService).completeSync(record);
        verify(syncedCounter, never()).increment();
        verify(failedCounter).increment();
        verify(syncRepository).save(failedWithStatus);
    }

    @Test
    void processPendingSyncs_shouldDoNothingWhenNoPendingRecords() {
        // Given
        when(syncRepository.findBySyncStatus(SyncStatus.PENDING)).thenReturn(List.of());

        // When
        processor.processPendingSyncs();

        // Then
        verify(syncRepository).findBySyncStatus(SyncStatus.PENDING);
        verify(persistenceService, never()).completeSync(any());
        verify(syncedCounter, never()).increment();
        verify(syncRepository, never()).save(any());
        verify(failedCounter, never()).increment();
    }

    @Test
    void processPendingSyncs_shouldProcessMultipleRecords() {
        // Given
        UserSyncRecord readyRecord1 = new UserSyncRecord(
            "id-5", "user1@example.com", "ext-5", "PASSENGER", List.of("basic"),
            SyncStatus.PENDING, Instant.now().minusSeconds(60), Instant.now(), 0, 5, null
        );

        UserSyncRecord readyRecord2 = new UserSyncRecord(
            "id-6", "user2@example.com", "ext-6", "PASSENGER", List.of("basic"),
            SyncStatus.PENDING, Instant.now().minusSeconds(60), Instant.now(), 0, 5, null
        );

        when(syncRepository.findBySyncStatus(SyncStatus.PENDING)).thenReturn(List.of(readyRecord1, readyRecord2));

        // When
        processor.processPendingSyncs();

        // Then
        verify(syncRepository).findBySyncStatus(SyncStatus.PENDING);
        verify(persistenceService).completeSync(readyRecord1);
        verify(persistenceService).completeSync(readyRecord2);
        verify(syncedCounter, times(2)).increment();
        verify(syncRepository, never()).save(any());
        verify(failedCounter, never()).increment();
    }

    @Test
    void processPendingSyncs_shouldHandleMixedReadyAndNotReadyRecords() {
        // Given
        UserSyncRecord readyRecord = new UserSyncRecord(
            "id-7", "ready@example.com", "ext-7", "PASSENGER", List.of("basic"),
            SyncStatus.PENDING, Instant.now().minusSeconds(60), Instant.now(), 0, 5, null
        );

        UserSyncRecord notReadyRecord = new UserSyncRecord(
            "id-8", "notready@example.com", "ext-8", "PASSENGER", List.of("basic"),
            SyncStatus.PENDING, Instant.now().minusSeconds(60), Instant.now(), 0, 5,
            Instant.now().plusSeconds(300)
        );

        when(syncRepository.findBySyncStatus(SyncStatus.PENDING)).thenReturn(List.of(readyRecord, notReadyRecord));

        // When
        processor.processPendingSyncs();

        // Then
        verify(syncRepository).findBySyncStatus(SyncStatus.PENDING);
        verify(persistenceService).completeSync(readyRecord);
        verify(persistenceService, never()).completeSync(notReadyRecord);
        verify(syncedCounter).increment();
        verify(syncRepository, never()).save(any());
        verify(failedCounter, never()).increment();
    }

    @Test
    void processPendingSyncs_shouldRetryAndThenComplete() {
        // Given
        UserSyncRecord record = mock(UserSyncRecord.class);
        UserSyncRecord updatedRecord = mock(UserSyncRecord.class);

        when(syncRepository.findBySyncStatus(SyncStatus.PENDING)).thenReturn(List.of(record));
        doThrow(new RuntimeException("Sync failed")).when(persistenceService).completeSync(record);
        when(record.isReady()).thenReturn(true);
        when(record.incrementRetry()).thenReturn(updatedRecord);
        when(updatedRecord.exceededMaxRetries()).thenReturn(false);

        // When - first call retries
        processor.processPendingSyncs();

        // Then - retry saved
        verify(syncRepository).save(updatedRecord);

        // Given - second call completes successfully
        when(syncRepository.findBySyncStatus(SyncStatus.PENDING)).thenReturn(List.of(updatedRecord));
        when(updatedRecord.isReady()).thenReturn(true);

        // When
        processor.processPendingSyncs();

        // Then
        verify(persistenceService).completeSync(updatedRecord);
        verify(syncedCounter).increment();
        verify(failedCounter, never()).increment();
    }
}
