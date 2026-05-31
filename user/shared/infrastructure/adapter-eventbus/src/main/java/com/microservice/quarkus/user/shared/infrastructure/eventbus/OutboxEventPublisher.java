package com.microservice.quarkus.user.shared.infrastructure.eventbus;

import com.microservice.quarkus.user.shared.application.outbox.EventScope;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEventRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@ApplicationScoped
public class OutboxEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

  @Inject
  OutboxEventRepository outboxEventRepository;

  @Inject
  EventBus eventBus;

  @Inject
  RabbitMqBroker rabbitMqBroker;

  @Inject
  MeterRegistry meterRegistry;

  @Inject
  Tracer tracer;

  @ConfigProperty(name = "eventbus.external.enabled", defaultValue = "true")
  boolean externalBusEnabled;

  private Counter eventsPublishedCounter;
  private Counter eventsFailedCounter;
  private Counter eventsDeadCounter;

  void onStart(@Observes @Priority(100) StartupEvent event) {
    eventsPublishedCounter = Counter.builder("outbox.events.published.total")
        .description("Total outbox events published successfully")
        .register(meterRegistry);
    eventsFailedCounter = Counter.builder("outbox.events.failed.total")
        .description("Total outbox event publish failures")
        .register(meterRegistry);
    eventsDeadCounter = Counter.builder("outbox.events.dead.total")
        .description("Total outbox events moved to DLQ")
        .register(meterRegistry);

    log.info("Outbox Publisher initialized for User Bounded Context");
  }

  @Scheduled(every = "1s")
  @Transactional
  public void publishPendingEvents() {
    List<OutboxEvent> pendingEvents = outboxEventRepository.findUnpublished();

    if (pendingEvents.isEmpty()) {
      return;
    }

    log.info("Outbox Publisher: {} pending events", pendingEvents.size());

    for (OutboxEvent event : pendingEvents) {
      if (event.isNotReady()) {
        continue;
      }

      MDC.put("correlationId", String.valueOf(event.getCorrelationId()));
      MDC.put("causationId", String.valueOf(event.getCausationId()));
      MDC.put("traceId", event.getTraceId() != null ? event.getTraceId() : "");
      MDC.put("spanId", event.getSpanId() != null ? event.getSpanId() : "");
      MDC.put("eventType", event.getEventType());
      MDC.put("aggregateId", event.getAggregateId());
      MDC.put("producer", event.getProducer());

      try {
        String topic = getTopicForEvent(event.getEventType());

        Span span = tracer.spanBuilder("outbox.publish." + event.getEventType())
            .setAttribute("event.type", event.getEventType())
            .setAttribute("event.aggregateId", event.getAggregateId())
            .setAttribute("event.producer", event.getProducer())
            .setAttribute("event.correlationId", event.getCorrelationId())
            .startSpan();

        if (event.getTraceId() != null && !event.getTraceId().isEmpty()
            && event.getSpanId() != null && !event.getSpanId().isEmpty()) {
          SpanContext linkedContext = createSpanContextFromHeaders(event.getTraceId(), event.getSpanId());
          if (linkedContext != null && linkedContext.isValid()) {
            span.addLink(linkedContext);
          }
        }

        try (Scope scope = span.makeCurrent()) {
          if (event.getScope() == EventScope.INTERNAL_ONLY || event.getScope() == EventScope.BOTH) {
            eventBus.publish(topic, event.getEventPayload());
            log.debug("Internal Bus: {}", topic);
          }

          if (externalBusEnabled
              && (event.getScope() == EventScope.EXTERNAL_ONLY || event.getScope() == EventScope.BOTH)) {
            rabbitMqBroker.publish(topic, event.getEventPayload(), event).toCompletableFuture().join();
            log.debug("RabbitMQ: {}", topic);
          }

          event.markAsPublished();
          outboxEventRepository.update(event);
          eventsPublishedCounter.increment();

          log.info("Published: [{}] {} v{} -> {} (correlationId={}, causationId={}, traceId={})",
              event.getProducer(), event.getEventType(), event.getEventVersion(), topic,
              event.getCorrelationId(), event.getCausationId(), event.getTraceId());
        } finally {
          span.end();
        }

      } catch (Exception e) {
        eventsFailedCounter.increment();

        if (event.shouldRetry()) {
          event.incrementRetry();
          outboxEventRepository.update(event);
          log.warn("Retry {}/{} for event {} [{}] — next retry at {}",
              event.getRetryCount(), event.getMaxRetries(),
              event.getId(), event.getProducer(), event.getNextRetryAt());
        } else {
          event.markAsDead(e.getMessage());
          outboxEventRepository.update(event);
          eventsDeadCounter.increment();
          log.error("DEAD event {} [{}] after {} retries: {}",
              event.getId(), event.getProducer(), event.getRetryCount(), e.getMessage());
        }
      } finally {
        MDC.clear();
      }
    }
  }

  @Scheduled(cron = "0 0 2 * * ?")
  @Transactional
  public void cleanOldEvents() {
    Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
    outboxEventRepository.deletePublishedBefore(threshold);
    log.info("Outbox: Events published before {} deleted", threshold);
  }

  private String getTopicForEvent(String eventType) {
    return switch (eventType) {
      case "identity.user.registered.v1" -> "identity.user.registered";
      case "identity.user.deleted.v1" -> "identity.user.deleted";
      case "notification.identity.user.registered.v1" -> "notification.identity.user.registered";
      case "admin.registered.v1" -> "admin.registered";
      case "notification.admin.registered.v1" -> "notification.admin.registered";
      case "passenger.registered.v1" -> "passenger.registered";
      case "notification.passenger.registered.v1" -> "notification.passenger.registered";
      default -> {
        log.warn("Unknown event type: {}", eventType);
        yield "unknown.event";
      }
    };
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
