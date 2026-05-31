package com.microservice.quarkus.user.identity.infrastructure.eventbus.consumer;

import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.api.KeycloakProvider;
import com.microservice.quarkus.user.identity.application.dto.KeycloakUserDTO;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.dto.UserType;
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
public class KeycloakReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(KeycloakReconciliationJob.class);

    @Inject
    KeycloakProvider keycloakProvider;

    @Inject
    IdentitySyncRepository syncRepository;

    @Inject
    IdentityPersistenceService persistenceService;

    @Inject
    MeterRegistry meterRegistry;

    private Counter reconciledCounter;

    void onStart(@jakarta.annotation.Priority(100) @jakarta.enterprise.event.Observes io.quarkus.runtime.StartupEvent ev) {
        reconciledCounter = Counter.builder("identity.reconciliation.orphans.total")
            .description("Total orphaned users found by reconciliation job")
            .register(meterRegistry);
    }

    @Scheduled(cron = "0 */5 * * * ?")  // every 5 minutes
    @Transactional
    public void reconcile() {
        log.debug("Reconciliation: Scanning Keycloak users vs DB records");

        try {
            var keycloakUsers = keycloakProvider.getAllUsers();
            int orphansFound = 0;

            for (KeycloakUserDTO kcUser : keycloakUsers) {
                UserSyncRecord existing = syncRepository.findByExternalId(kcUser.externalId());
                if (existing == null) {
                    // Also check by email — might have a PENDING record without externalId
                    UserSyncRecord byEmail = syncRepository.findByEmail(kcUser.email());
                    if (byEmail != null && byEmail.externalId() == null) {
                        // Update existing PENDING record with the externalId
                        syncRepository.save(byEmail.withExternalId(kcUser.externalId()));
                        log.info("Reconciliation: Updated PENDING record for {} with externalId={}",
                            kcUser.email(), kcUser.externalId());
                        continue;
                    }

                    // True orphan — create PENDING record, processor will complete it
                    UserType type = guessUserType(kcUser);
                    UserSyncRecord record = UserSyncRecord.createNew(kcUser.email(), type)
                        .withExternalId(kcUser.externalId())
                        .withSyncStatus(SyncStatus.PENDING);
                    syncRepository.save(record);
                    orphansFound++;
                    log.info("Reconciliation: Found orphan user {} (externalId={}) — created PENDING record",
                        kcUser.email(), kcUser.externalId());
                }
            }

            if (orphansFound > 0) {
                reconciledCounter.increment(orphansFound);
                log.info("Reconciliation: Found {} orphaned users", orphansFound);
            } else {
                log.debug("Reconciliation: No orphans found");
            }
        } catch (Exception e) {
            log.error("Reconciliation: Failed to query Keycloak — {}", e.getMessage());
        }
    }

    private UserType guessUserType(KeycloakUserDTO kcUser) {
        // Default to PASSENGER if we can't determine — processor/consumers will handle
        return UserType.PASSENGER;
    }
}
