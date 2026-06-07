package com.microservice.quarkus.user.passenger.infrastructure.eventbus.consumer;

import com.microservice.quarkus.user.passenger.application.service.PassengerService;
import com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl.UserDeletedEventDTO;
import com.microservice.quarkus.user.shared.application.outbox.IdempotencyStore;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@ApplicationScoped
public class UserDeletedListener {

  private static final Logger log = LoggerFactory.getLogger(UserDeletedListener.class);

  private final PassengerService passengerService;
  private final IdempotencyStore idempotencyStore;

  @Inject
  Tracer tracer;

  @Inject
  public UserDeletedListener(PassengerService passengerService, IdempotencyStore idempotencyStore) {
    this.passengerService = passengerService;
    this.idempotencyStore = idempotencyStore;
  }

  @ConsumeEvent("identity.user.deleted.v1")
  @Blocking
  public void onUserDeleted(String event) {
    UserDeletedEventDTO dto = new JsonObject(event).mapTo(UserDeletedEventDTO.class);

    if (dto.correlationId() != null) {
      MDC.put("correlationId", dto.correlationId());
    }
    if (dto.traceId() != null) MDC.put("traceId", dto.traceId());
    if (dto.spanId() != null) MDC.put("spanId", dto.spanId());

    SpanContext parentCtx = createSpanContextFromHeaders(dto.traceId(), dto.spanId());
    Span span;
    if (parentCtx != null && parentCtx.isValid()) {
      span = tracer.spanBuilder("passenger.onUserDeleted")
          .setParent(Context.root().with(Span.wrap(parentCtx)))
          .startSpan();
    } else {
      span = tracer.spanBuilder("passenger.onUserDeleted").startSpan();
    }

    try (Scope scope = span.makeCurrent()) {
      if (dto.eventId() == null) {
        log.warn("Passenger Module: Missing eventId — skipping invalid event");
        return;
      }

      if (!idempotencyStore.tryAcquire(dto.eventId().toString(), "passenger-delete-inbound")) {
        log.info("Passenger Module: Idempotent skip — eventId={} already processed", dto.eventId());
        return;
      }

      log.info("Passenger Module: Received delete event for aggregateId={}", dto.aggregateId());

      try {
        passengerService.deleteByExternalId(dto.aggregateId());
        log.info("Passenger Module: Passenger deleted for aggregateId={}", dto.aggregateId());
      } catch (Exception e) {
        log.error("Passenger Module: Failed to delete passenger for aggregateId={}", dto.aggregateId(), e);
        throw e;
      }
    } finally {
      span.end();
      MDC.clear();
    }
  }

  private SpanContext createSpanContextFromHeaders(String traceId, String spanId) {
    try {
      return SpanContext.createFromRemoteParent(
          traceId,
          spanId,
          TraceFlags.getSampled(),
          TraceState.getDefault());
    } catch (Exception e) {
      log.debug("Failed to create span context from traceId={}, spanId={}", traceId, spanId);
      return null;
    }
  }
}
