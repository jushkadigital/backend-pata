package com.microservice.quarkus.user.shared.infrastructure.eventbus;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.microservice.quarkus.user.shared.application.outbox.EventScope;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.application.outbox.OutboxEventRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    OutboxEventRepository outboxEventRepository;

    @Mock
    EventBus eventBus;

    @Mock
    RabbitMqBroker rabbitMqBroker;

    @Mock
    Tracer tracer;

    private OutboxEvent createEvent(EventScope scope, String eventType) {
        return OutboxEvent.create(
            eventType, 1, "User", "agg-1",
            "corr-1", null, "trace-1", "span-1",
            "user-service", null, null,
            "{}", scope, Instant.now());
    }

    private OutboxEventPublisher createPublisher(boolean externalEnabled) {
        OutboxEventPublisher publisher = new OutboxEventPublisher();
        publisher.outboxEventRepository = outboxEventRepository;
        publisher.eventBus = eventBus;
        publisher.rabbitMqBroker = rabbitMqBroker;
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        publisher.meterRegistry = meterRegistry;
        publisher.externalBusEnabled = externalEnabled;

        SpanBuilder spanBuilder = mock(SpanBuilder.class);
        Span span = mock(Span.class);
        Scope scope = mock(Scope.class);
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
        publisher.tracer = tracer;

        setPrivateField(publisher, "eventsPublishedCounter", meterRegistry.counter("test.published"));
        setPrivateField(publisher, "eventsFailedCounter", meterRegistry.counter("test.failed"));
        setPrivateField(publisher, "eventsDeadCounter", meterRegistry.counter("test.dead"));
        return publisher;
    }

    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void publishPendingEvents_shouldPublishToEventBusAndRabbitMqWhenExternalEnabledAndBoth() {
        OutboxEvent event = createEvent(EventScope.BOTH, "identity.user.created.v1");
        when(outboxEventRepository.findUnpublished()).thenReturn(List.of(event));
        when(rabbitMqBroker.publish(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        createPublisher(true).publishPendingEvents();

        verify(eventBus).publish(eq("identity.user.created.v1"), anyString());
        verify(rabbitMqBroker).publish(eq("identity.user.created.v1"), anyString(), any());
    }

    @Test
    void publishPendingEvents_shouldPublishToEventBusOnlyWhenExternalDisabled() {
        OutboxEvent event = createEvent(EventScope.BOTH, "identity.user.created.v1");
        when(outboxEventRepository.findUnpublished()).thenReturn(List.of(event));

        createPublisher(false).publishPendingEvents();

        verify(eventBus).publish(eq("identity.user.created.v1"), anyString());
        verify(rabbitMqBroker, never()).publish(anyString(), anyString(), any());
    }

    @Test
    void publishPendingEvents_shouldPublishInternalOnlyToEventBusOnlyEvenWhenExternalEnabled() {
        OutboxEvent event = createEvent(EventScope.INTERNAL_ONLY, "identity.user.created.v1");
        when(outboxEventRepository.findUnpublished()).thenReturn(List.of(event));

        createPublisher(true).publishPendingEvents();

        verify(eventBus).publish(eq("identity.user.created.v1"), anyString());
        verify(rabbitMqBroker, never()).publish(anyString(), anyString(), any());
    }

    @Test
    void publishPendingEvents_shouldPublishExternalOnlyToRabbitMqOnlyWhenEnabled() {
        OutboxEvent event = createEvent(EventScope.EXTERNAL_ONLY, "notification.identity.user.created.v1");
        when(outboxEventRepository.findUnpublished()).thenReturn(List.of(event));
        when(rabbitMqBroker.publish(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        createPublisher(true).publishPendingEvents();

        verify(eventBus, never()).publish(anyString(), anyString());
        verify(rabbitMqBroker).publish(eq("notification.identity.user.created.v1"), anyString(), any());
    }

    @Test
    void publishPendingEvents_shouldNotPublishExternalOnlyWhenRabbitMqDisabled() {
        OutboxEvent event = createEvent(EventScope.EXTERNAL_ONLY, "notification.identity.user.created.v1");
        when(outboxEventRepository.findUnpublished()).thenReturn(List.of(event));

        createPublisher(false).publishPendingEvents();

        verify(eventBus, never()).publish(anyString(), anyString());
        verify(rabbitMqBroker, never()).publish(anyString(), anyString(), any());
    }

    @Test
    void publishPendingEvents_shouldRouteDeletedEventToCorrectTopic() {
        OutboxEvent event = createEvent(EventScope.BOTH, "identity.user.deleted.v1");
        when(outboxEventRepository.findUnpublished()).thenReturn(List.of(event));
        when(rabbitMqBroker.publish(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        createPublisher(true).publishPendingEvents();

        verify(eventBus).publish(eq("identity.user.deleted.v1"), anyString());
        verify(rabbitMqBroker).publish(eq("identity.user.deleted.v1"), anyString(), any());
    }
}
