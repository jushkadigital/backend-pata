package com.microservice.quarkus.user.admin.infrastructure.eventbus.consumer;

import com.microservice.quarkus.user.admin.application.service.AdminService;
import com.microservice.quarkus.user.admin.infrastructure.eventbus.acl.UserDeletedEventDTO;

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

  private final AdminService adminService;

  @Inject
  Tracer tracer;

  @Inject
  public UserDeletedListener(AdminService adminService) {
    this.adminService = adminService;
  }

  @ConsumeEvent("identity.user.deleted")
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
      span = tracer.spanBuilder("admin.onUserDeleted")
          .setParent(Context.root().with(Span.wrap(parentCtx)))
          .startSpan();
    } else {
      span = tracer.spanBuilder("admin.onUserDeleted").startSpan();
    }

    try (Scope scope = span.makeCurrent()) {
      log.info("Admin Module: Received delete event for aggregateId={}", dto.aggregateId());

      try {
        adminService.deleteByExternalId(dto.aggregateId());
        log.info("Admin Module: Admin deleted for aggregateId={}", dto.aggregateId());
      } catch (Exception e) {
        log.error("Admin Module: Failed to delete admin for aggregateId={}", dto.aggregateId(), e);
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
