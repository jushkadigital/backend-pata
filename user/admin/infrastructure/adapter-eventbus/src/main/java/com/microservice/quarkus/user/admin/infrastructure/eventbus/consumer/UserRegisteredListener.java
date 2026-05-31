package com.microservice.quarkus.user.admin.infrastructure.eventbus.consumer;

import com.microservice.quarkus.user.admin.application.dto.CreateAdminCommand;
import com.microservice.quarkus.user.admin.application.service.AdminService;
import com.microservice.quarkus.user.admin.infrastructure.eventbus.acl.UserRegisteredEventDTO;
import com.microservice.quarkus.user.admin.infrastructure.eventbus.acl.UserRegisteredEventMapper;
import com.microservice.quarkus.user.identity.application.api.GroupIdentityProvider;

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
import jakarta.persistence.EntityExistsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

@ApplicationScoped
public class UserRegisteredListener {

  private static final Logger log = LoggerFactory.getLogger(UserRegisteredListener.class);

  private static final Map<String, String> ADMIN_TYPE_TO_GROUP = Map.of(
      "SUPER_ADMIN", "admin/super-admin",
      "ADMIN", "admin/admin",
      "EDITOR", "admin/editor");

  private final AdminService adminService;
  private final UserRegisteredEventMapper eventMapper;
  private final GroupIdentityProvider groupIdentityProvider;

  @Inject
  Tracer tracer;

  @Inject
  public UserRegisteredListener(AdminService adminService,
                                 UserRegisteredEventMapper eventMapper,
                                 GroupIdentityProvider groupIdentityProvider) {
    this.adminService = adminService;
    this.eventMapper = eventMapper;
    this.groupIdentityProvider = groupIdentityProvider;
  }

  @ConsumeEvent("identity.user.registered")
  @Blocking
  public void onUserRegistered(String event) {

    UserRegisteredEventDTO dto = new JsonObject(event).mapTo(UserRegisteredEventDTO.class);

    if (dto.correlationId() != null) {
      MDC.put("correlationId", dto.correlationId());
    }
    if (dto.traceId() != null) MDC.put("traceId", dto.traceId());
    if (dto.spanId() != null) MDC.put("spanId", dto.spanId());

    SpanContext parentCtx = createSpanContextFromHeaders(dto.traceId(), dto.spanId());
    Span span;
    if (parentCtx != null && parentCtx.isValid()) {
      span = tracer.spanBuilder("admin.onUserRegistered")
          .setParent(Context.root().with(Span.wrap(parentCtx)))
          .startSpan();
    } else {
      span = tracer.spanBuilder("admin.onUserRegistered").startSpan();
    }

    try (Scope scope = span.makeCurrent()) {
      log.info("Admin Module: Received event for user {} type {}", dto.email(), dto.type());

      if (!"ADMIN".equals(dto.type())) {
        log.debug("Admin Module: Ignoring non-admin event (type={})", dto.type());
        return;
      }

      try {
        CreateAdminCommand cmd = eventMapper.toCommand(dto);
        adminService.register(cmd, dto.correlationId());
        log.info("Admin Module: Admin created for aggregateId={}", dto.aggregateId());
      } catch (EntityExistsException e) {
        log.info("Admin Module: Idempotent skip — admin already exists for aggregateId={}", dto.aggregateId());
      }

      // Assign Keycloak group directly — Admin owns its type-to-group mapping
      assignGroup(dto);

    } finally {
      span.end();
      MDC.clear();
    }
  }

  private void assignGroup(UserRegisteredEventDTO dto) {
    String groupPath = ADMIN_TYPE_TO_GROUP.get(dto.subType());
    if (groupPath == null) {
      log.error("Admin Module: Unknown admin subType: {} — skipping group assignment", dto.subType());
      return;
    }

    String groupId = groupIdentityProvider.findGroupByPath(groupPath);
    if (groupId == null) {
      log.error("Admin Module: Group not found for path: {} — skipping group assignment", groupPath);
      return;
    }

    groupIdentityProvider.assignUserToGroup(dto.aggregateId(), groupId);
    log.info("Admin Module: User {} assigned to group {}", dto.email(), groupPath);
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
