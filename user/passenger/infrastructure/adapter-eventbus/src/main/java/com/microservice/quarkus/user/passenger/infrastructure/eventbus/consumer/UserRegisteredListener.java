package com.microservice.quarkus.user.passenger.infrastructure.eventbus.consumer;

import com.microservice.quarkus.user.passenger.application.api.PassengerRepository;
import com.microservice.quarkus.user.passenger.application.dto.CreatePassengerCommand;
import com.microservice.quarkus.user.passenger.application.service.PassengerService;
import com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl.UserRegisteredEventDTO;
import com.microservice.quarkus.user.passenger.infrastructure.eventbus.acl.UserRegisteredEventMapper;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

@ApplicationScoped
public class UserRegisteredListener {

  private static final Logger log = LoggerFactory.getLogger(UserRegisteredListener.class);

  private static final Map<String, String> PASSENGER_TYPE_TO_GROUP = Map.of(
      "PREMIUM", "passenger/premium",
      "STANDARD", "passenger/standard",
      "BASIC", "passenger/basic");

  private final PassengerService passengerService;
  private final PassengerRepository passengerRepository;
  private final UserRegisteredEventMapper eventMapper;
  private final GroupIdentityProvider groupIdentityProvider;

  @Inject
  Tracer tracer;

  @Inject
  public UserRegisteredListener(PassengerService passengerService,
                                 PassengerRepository passengerRepository,
                                 UserRegisteredEventMapper eventMapper,
                                 GroupIdentityProvider groupIdentityProvider) {
    this.passengerService = passengerService;
    this.passengerRepository = passengerRepository;
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
      span = tracer.spanBuilder("passenger.onUserRegistered")
          .setParent(Context.root().with(Span.wrap(parentCtx)))
          .startSpan();
    } else {
      span = tracer.spanBuilder("passenger.onUserRegistered").startSpan();
    }

    try (Scope scope = span.makeCurrent()) {
      log.info("Passenger Module: Received event for user {} type {}", dto.email(), dto.type());

      if (!"PASSENGER".equals(dto.type())) {
        log.debug("Passenger Module: Ignoring non-passenger event (type={})", dto.type());
        return;
      }

      if (passengerRepository.findByExternalId(dto.aggregateId()).isPresent()) {
        log.info("Passenger Module: Idempotent skip — passenger already exists for aggregateId={}", dto.aggregateId());
      } else {
        CreatePassengerCommand cmd = eventMapper.toCommand(dto);
        passengerService.register(cmd, dto.correlationId());
        log.info("Passenger Module: Passenger created for aggregateId={}", dto.aggregateId());
      }

      // Assign Keycloak group directly — Passenger owns its type-to-group mapping
      assignGroup(dto);

    } finally {
      span.end();
      MDC.clear();
    }
  }

  private void assignGroup(UserRegisteredEventDTO dto) {
    String groupPath = PASSENGER_TYPE_TO_GROUP.get(dto.subType());
    if (groupPath == null) {
      log.error("Passenger Module: Unknown passenger subType: {} — skipping group assignment", dto.subType());
      return;
    }

    String groupId = groupIdentityProvider.findGroupByPath(groupPath);
    if (groupId == null) {
      log.error("Passenger Module: Group not found for path: {} — skipping group assignment", groupPath);
      return;
    }

    groupIdentityProvider.assignUserToGroup(dto.aggregateId(), groupId);
    log.info("Passenger Module: User {} assigned to group {}", dto.email(), groupPath);
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
