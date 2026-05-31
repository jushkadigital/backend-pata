package com.microservice.quarkus.user.identity.infrastructure.webhook.consumer;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

import org.jboss.logging.Logger;

import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.api.KeycloakProvider;
import com.microservice.quarkus.user.identity.application.dto.CreateUserCommand;
import com.microservice.quarkus.user.identity.application.dto.KeycloakUserDTO;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import com.microservice.quarkus.user.identity.application.service.UserService;
import com.microservice.quarkus.user.identity.infrastructure.webhook.event.WebhookKeycloakPayload;
import com.microservice.quarkus.user.identity.infrastructure.webhook.event.WebhookPayloadPayload;
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

  private static final Map<String, String> CLI_NAME_DICT = Map.of(
      "dashboard-client", "ADMIN",
      "frontend-client", "PASSENGER");

  private final ClientIdentityProvider clientService;
  private final KeycloakProvider keycloakProvider;
  private final UserService userService;
  private final CorrelationIdInterceptor correlationIdInterceptor;
  private final IdentitySyncRepository syncRepository;

  @Inject Tracer tracer;

  @Inject
  public KeycloakWebhookConsumer(ClientIdentityProvider clientService,
                                  KeycloakProvider keycloakProvider,
                                  UserService userService,
                                  CorrelationIdInterceptor correlationIdInterceptor,
                                  IdentitySyncRepository syncRepository) {
    this.clientService = clientService;
    this.keycloakProvider = keycloakProvider;
    this.userService = userService;
    this.correlationIdInterceptor = correlationIdInterceptor;
    this.syncRepository = syncRepository;
  }

  @ConsumeEvent("identity.webhook.keycloak")
  @Blocking
  public void processKeycloakEvent(WebhookKeycloakPayload payload) {

    LOG.infof("Procesando evento: %s para usuario %s", payload.eventType(), payload.userId());

    if (payload.traceParent() != null) {
        MDC.put("traceParent", payload.traceParent());
        // Extract traceId from traceparent header (format: version-traceId-spanId-flags)
        // The traceId is the second segment (32 hex chars)
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
      // Idempotency check: skip if user is already SYNCED or PENDING
      UserSyncRecord existingRecord = syncRepository.findByExternalId(payload.userId());
      if (existingRecord == null) {
        existingRecord = syncRepository.findByEmail(payload.userId());
      }
      if (existingRecord != null && (SyncStatus.SYNCED.equals(existingRecord.syncStatus()) ||
          SyncStatus.PENDING.equals(existingRecord.syncStatus()))) {
        LOG.infof("Skipping webhook event for user %s - already %s", payload.userId(), existingRecord.syncStatus());
        return;
      }

      if ("REGISTER".equals(payload.eventType())) {
        KeycloakUserDTO tempUser = keycloakProvider.getUserById(payload.userId());
        CreateUserCommand command = CreateUserCommand.fromWebhook(
            tempUser.email(),
            tempUser.externalId(),
            CLI_NAME_DICT.get(clientService.getClientNameById(payload.clientId())));

        String correlationId = correlationIdInterceptor.getOrCreateCorrelationId();
        userService.register(command, correlationId);
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
      CreateUserCommand command = CreateUserCommand.fromWebhook2(payload.email(), payload.password(),
          CLI_NAME_DICT.get(clientService.getClientNameById(payload.clientId())), payload.type());

      String correlationId = correlationIdInterceptor.getOrCreateCorrelationId();
      userService.register(command, correlationId);
      LOG.info("Sincronización exitosa.");
    } catch (Exception e) {
      span.recordException(e);
      LOG.error("Fallo al procesar webhook. Reintentando...", e);
      throw e;
    } finally {
      span.end();
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
