package com.microservice.quarkus.user.identity.infrastructure.eventbus.consumer;

import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.service.IdentityPersistenceService;

import io.quarkus.scheduler.Scheduled;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SyncStatusProcessor {

    private static final Logger log = LoggerFactory.getLogger(SyncStatusProcessor.class);

    @Inject
    IdentitySyncRepository syncRepository;

    @Inject
    IdentityPersistenceService persistenceService;

    @Inject
    MeterRegistry meterRegistry;

    private Counter syncedCounter;
    private Counter failedCounter;

    void onStart(@jakarta.annotation.Priority(100) @jakarta.enterprise.event.Observes io.quarkus.runtime.StartupEvent ev) {
        syncedCounter = Counter.builder("identity.sync.completed.total")
            .description("Total user syncs completed from PENDING to SYNCED")
            .register(meterRegistry);
        failedCounter = Counter.builder("identity.sync.failed.total")
            .description("Total user syncs moved from PENDING to FAILED after max retries")
            .register(meterRegistry);
    }

    @Scheduled(every = "2s")
    @Transactional
    public void processPendingSyncs() {
        var pendingRecords = syncRepository.findBySyncStatus(SyncStatus.PENDING);
        if (pendingRecords.isEmpty()) {
            return;
        }

        log.debug("Sync Processor: {} PENDING records", pendingRecords.size());

        for (UserSyncRecord record : pendingRecords) {
            if (!record.isReady()) {
                continue;  // Not yet time for retry
            }

            try {
                persistenceService.completeSync(record);
                syncedCounter.increment();
                log.info("Sync Processor: Completed sync for {} (externalId={})",
                    record.email(), record.externalId());
            } catch (Exception e) {
                log.warn("Sync Processor: Failed to complete sync for {} — {}", record.email(), e.getMessage());

                UserSyncRecord updated = record.incrementRetry();
                if (updated.exceededMaxRetries()) {
                    syncRepository.save(updated.withSyncStatus(SyncStatus.FAILED));
                    failedCounter.increment();
                    log.error("Sync Processor: DEAD sync for {} after {} retries: {}",
                        record.email(), updated.retryCount(), e.getMessage());
                } else {
                    syncRepository.save(updated);
                    log.info("Sync Processor: Retry {}/{} for {} — next retry at {}",
                        updated.retryCount(), updated.maxRetries(),
                        record.email(), updated.nextRetryAt());
                }
            }
        }
    }
}
