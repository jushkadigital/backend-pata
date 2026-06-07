package com.microservice.quarkus.user.identity.application.service;

import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.api.KeycloakProvider;
import com.microservice.quarkus.user.identity.application.dto.CreateUserCommand;
import com.microservice.quarkus.user.identity.application.dto.KeycloakUserDTO;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.event.IdentityUserCreated;
import com.microservice.quarkus.user.shared.application.outbox.EventMetadata;
import com.microservice.quarkus.user.shared.application.outbox.TraceContextProvider;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

import com.microservice.quarkus.user.shared.domain.DomainEvent;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationScoped
public class UserService {

  private final KeycloakProvider keycloakClient;
  private final IdentitySyncRepository syncRepository;
  private final IdentityPersistenceService persistenceService;
  private final TraceContextProvider traceContextProvider;

  private final List<DomainEvent> domainEvents = new ArrayList<>();

  public UserService(KeycloakProvider keycloakClient,
                     IdentitySyncRepository syncRepository,
                     IdentityPersistenceService persistenceService,
                     TraceContextProvider traceContextProvider) {
    this.keycloakClient = keycloakClient;
    this.syncRepository = syncRepository;
    this.persistenceService = persistenceService;
    this.traceContextProvider = traceContextProvider;
  }

  public String register(CreateUserCommand cmd) {
    return register(cmd, null, null, null);
  }

  public String register(CreateUserCommand cmd, String correlationId) {
    return register(cmd, correlationId, null, null);
  }

  @WithSpan("identity.user.register")
  public String register(CreateUserCommand cmd, String correlationId, String causationId, String actorId) {
    UserSyncRecord existing = syncRepository.findByEmail(cmd.email());
    if (existing != null && existing.syncStatus() == SyncStatus.SYNCED) {
      return existing.externalId();
    }
    // Also skip if PENDING — already being processed
    if (existing != null && existing.syncStatus() == SyncStatus.PENDING) {
      return existing.externalId();
    }

    List<String> roles = new ArrayList<>(cmd.roles());
    String userType = cmd.userType();
    String effectiveCorrelationId = correlationId != null ? correlationId : java.util.UUID.randomUUID().toString();
    EventMetadata traceMeta = traceContextProvider.current();

    String externalId = null;
    boolean keycloakFailed = false;
    try {
      if (!cmd.isFromKeycloak()) {
        externalId = keycloakClient.createUser(cmd.email(), cmd.password());
      } else {
        externalId = cmd.externalId();
      }
    } catch (Exception e) {
      keycloakFailed = true;
    }

    if (keycloakFailed) {
      persistenceService.persistFailedSync(cmd.email(), userType, roles);
      return null;
    }

    if (cmd.isFromKeycloak()) {
      // Webhook flow — immediate sync (user already exists in Keycloak)
      IdentityUserCreated domainEvent = new IdentityUserCreated(
          externalId, cmd.email(), userType, roles);
      persistenceService.persistSyncAndOutbox(cmd.email(), userType, roles, externalId, domainEvent,
          effectiveCorrelationId, causationId, traceMeta, actorId);
    } else {
      // Admin/API flow — phased sync (Phase 1 only, processor handles Phase 2)
      persistenceService.persistPendingSync(cmd.email(), userType, roles, externalId,
          effectiveCorrelationId, causationId, traceMeta, actorId);
    }

    return externalId;
  }

  public List<DomainEvent> drainDomainEvents() {
    List<DomainEvent> events = new ArrayList<>(domainEvents);
    domainEvents.clear();
    return events;
  }

  public List<KeycloakUserDTO> allUsersKeycloak() {
    return keycloakClient.getAllUsers();
  }

  @WithSpan("identity.user.delete")
  public void deleteByExternalId(String externalId, String correlationId) {
    persistenceService.deleteSyncRecord(externalId, correlationId);
  }
}
