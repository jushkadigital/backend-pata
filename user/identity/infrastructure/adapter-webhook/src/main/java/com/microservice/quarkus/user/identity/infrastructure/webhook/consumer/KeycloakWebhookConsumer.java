package com.microservice.quarkus.user.identity.infrastructure.webhook.consumer;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.GroupIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.api.KeycloakProvider;
import com.microservice.quarkus.user.identity.application.api.RoleIdentityProvider;
import com.microservice.quarkus.user.identity.application.dto.CreateUserCommand;
import com.microservice.quarkus.user.identity.application.dto.KeycloakUserDTO;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.dto.UserType;
import com.microservice.quarkus.user.identity.application.service.UserService;
import com.microservice.quarkus.user.identity.infrastructure.webhook.event.WebhookKeycloakPayload;
import com.microservice.quarkus.user.identity.infrastructure.webhook.event.WebhookPayloadPayload;
import com.microservice.quarkus.user.shared.application.outbox.IdempotencyStore;
import com.microservice.quarkus.user.shared.application.outbox.TraceContextProvider;
import com.microservice.quarkus.user.shared.infrastructure.eventbus.CorrelationIdInterceptor;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.MDC;

@ApplicationScoped
public class KeycloakWebhookConsumer {

  private static final Logger LOG = Logger.getLogger(KeycloakWebhookConsumer.class);

  private static final Map<String, Set<String>> CLIENT_DEFAULT_ROLES = Map.of(
      "dashboard-client", Set.of("editor"),
      "frontend-client", Set.of("basic"));

  private static final Map<String, String> COMPOSITE_ROLE_TO_GROUP = Map.of(
      "super-admin", "admins",
      "admin", "admins",
      "editor", "admins",
      "premium", "passengers",
      "standard", "passengers",
      "basic", "passengers");

  private static final Map<String, String> COMPOSITE_ROLE_TO_CLIENT_ID = Map.of(
      "super-admin", "dashboard-client",
      "admin", "dashboard-client",
      "editor", "dashboard-client",
      "premium", "frontend-client",
      "standard", "frontend-client",
      "basic", "frontend-client");

  private final ClientIdentityProvider clientService;
  private final KeycloakProvider keycloakProvider;
  private final UserService userService;
  private final CorrelationIdInterceptor correlationIdInterceptor;
  private final IdentitySyncRepository syncRepository;
  private final IdempotencyStore idempotencyStore;
  private final GroupIdentityProvider groupIdentityProvider;
  private final RoleIdentityProvider roleIdentityProvider;
  private final TraceContextProvider traceContextProvider;

  @Inject Tracer tracer;

  @Inject
  public KeycloakWebhookConsumer(ClientIdentityProvider clientService,
                                  KeycloakProvider keycloakProvider,
                                  UserService userService,
                                  CorrelationIdInterceptor correlationIdInterceptor,
                                  IdentitySyncRepository syncRepository,
                                  IdempotencyStore idempotencyStore,
                                  GroupIdentityProvider groupIdentityProvider,
                                  RoleIdentityProvider roleIdentityProvider,
                                  TraceContextProvider traceContextProvider) {
    this.clientService = clientService;
    this.keycloakProvider = keycloakProvider;
    this.userService = userService;
    this.correlationIdInterceptor = correlationIdInterceptor;
    this.syncRepository = syncRepository;
    this.idempotencyStore = idempotencyStore;
    this.groupIdentityProvider = groupIdentityProvider;
    this.roleIdentityProvider = roleIdentityProvider;
    this.traceContextProvider = traceContextProvider;
  }

  @ConsumeEvent("identity.webhook.keycloak")
  @Blocking
  public void processKeycloakEvent(WebhookKeycloakPayload payload) {

    LOG.infof("Procesando evento: %s para usuario %s", payload.eventType(), payload.userId());

    if (payload.traceParent() != null) {
        MDC.put("traceParent", payload.traceParent());
        String[] parts = payload.traceParent().split("-");
        if (parts.length >= 2) {
            MDC.put("traceId", parts[1]);
        }
        if (parts.length >= 3) {
            MDC.put("spanId", parts[2]);
        }
    }

    SpanContext parentContext = extractParentContextFromTraceParent(payload.traceParent());
    Span span;
    if (parentContext != null && parentContext.isValid()) {
        span = tracer.spanBuilder("webhook.onKeycloakEvent")
            .setParent(Context.root().with(Span.wrap(parentContext)))
            .startSpan();
    } else {
        span = tracer.spanBuilder("webhook.onKeycloakEvent").startSpan();
    }

    try (Scope scope = span.makeCurrent()) {
      String idempotencyKey = payload.userId() + ":" + payload.eventType();
      if (!idempotencyStore.tryAcquire(idempotencyKey, "identity-webhook-inbound")) {
        LOG.infof("Idempotent skip — webhook already processed: user=%s type=%s",
            payload.userId(), payload.eventType());
        return;
      }

      if ("REGISTER".equals(payload.eventType())) {
        UserSyncRecord existingRecord = syncRepository.findByExternalId(payload.userId());
        if (existingRecord == null) {
          existingRecord = syncRepository.findByEmail(payload.userId());
        }
        if (existingRecord != null && (SyncStatus.SYNCED.equals(existingRecord.syncStatus()) ||
            SyncStatus.PENDING.equals(existingRecord.syncStatus()))) {
          LOG.infof("Skipping REGISTER webhook event for user %s - already %s", payload.userId(), existingRecord.syncStatus());
          return;
        }
        KeycloakUserDTO tempUser = keycloakProvider.getUserById(payload.userId());
        Set<String> roles = CLIENT_DEFAULT_ROLES.get(clientService.getClientNameById(payload.clientId()));
        if (roles == null) {
          roles = Set.of("basic");
        }
        String userType = UserType.fromCompositeRoles(roles).name();
        CreateUserCommand command = CreateUserCommand.fromWebhook(
            tempUser.email(),
            tempUser.externalId(),
            userType,
            roles);

        String correlationId = correlationIdInterceptor.getOrCreateCorrelationId();
        String externalId = userService.register(command, correlationId);
        if (externalId != null) {
          assignGroupsAndRoles(externalId, roles);
        }
        LOG.info("Sincronización exitosa.");
      } else if ("DELETE".equals(payload.eventType())) {
        String correlationId = correlationIdInterceptor.getOrCreateCorrelationId();
        userService.deleteByExternalId(payload.userId(), correlationId);
        LOG.info("Eliminación sincronizada.");
      }
    } catch (Exception e) {
      span.recordException(e);
      LOG.error("Fallo al procesar webhook. Reintentando...", e);
      throw e;
    } finally {
      span.end();
      MDC.clear();
    }
  }

  @ConsumeEvent("identity.webhook.payload")
  @Blocking
  public void processPayloadEvent(WebhookPayloadPayload payload) {

    LOG.infof("Procesando evento para usuario %s", payload.email());

    Span span = tracer.spanBuilder("webhook.onPayloadEvent").startSpan();

    try (Scope scope = span.makeCurrent()) {
      Set<String> roles = CLIENT_DEFAULT_ROLES.get(clientService.getClientNameById(payload.clientId()));
      if (roles == null) {
        roles = Set.of("basic");
      }

      String specificRole = payload.role();
      if (specificRole != null && !specificRole.isBlank()) {
        roles = Set.of(specificRole);
      }

      String userType = UserType.fromCompositeRoles(roles).name();
      CreateUserCommand command = CreateUserCommand.fromWebhook2(payload.email(), payload.password(), userType, roles);

      String correlationId = correlationIdInterceptor.getOrCreateCorrelationId();
      String externalId = userService.register(command, correlationId);
      if (externalId != null) {
        assignGroupsAndRoles(externalId, roles);
      }
      LOG.info("Sincronización exitosa.");
    } catch (Exception e) {
      span.recordException(e);
      LOG.error("Fallo al procesar webhook. Reintentando...", e);
      throw e;
    } finally {
      span.end();
    }
  }

  private void assignGroupsAndRoles(String externalId, Set<String> roles) {
    for (String role : roles) {
      String groupPath = COMPOSITE_ROLE_TO_GROUP.get(role);
      if (groupPath != null) {
        String groupId = groupIdentityProvider.findGroupByPath(groupPath);
        if (groupId != null) {
          groupIdentityProvider.assignUserToGroup(externalId, groupId);
          LOG.infof("Usuario %s asignado al grupo %s", externalId, groupPath);
        } else {
          LOG.warnf("Grupo no encontrado: %s", groupPath);
        }
      }

      String targetClientId = COMPOSITE_ROLE_TO_CLIENT_ID.get(role);
      if (targetClientId != null) {
        try {
          roleIdentityProvider.assignClientRoleToUser(externalId, targetClientId, role);
          LOG.infof("Rol '%s' asignado al usuario %s", role, externalId);
        } catch (Exception e) {
          LOG.warnf("Error asignando rol '%s' al usuario %s: %s", role, externalId, e.getMessage());
        }
      }
    }
  }

  private SpanContext extractParentContextFromTraceParent(String traceParent) {
      if (traceParent == null || traceParent.isBlank()) {
          return null;
      }
      try {
          String[] parts = traceParent.split("-");
          if (parts.length >= 3) {
              return SpanContext.createFromRemoteParent(
                  parts[1], parts[2], TraceFlags.getSampled(), TraceState.getDefault());
          }
      } catch (Exception e) {
          LOG.debugf("Failed to extract span context from traceparent: %s", traceParent);
      }
      return null;
  }
}
