package com.microservice.quarkus.user.shared.infrastructure.eventbus;

import com.microservice.quarkus.user.shared.application.outbox.OutboxEvent;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class RabbitMqBroker {

  @Inject
  @Channel("domain-events-out")
  Emitter<String> emitter;

  public CompletionStage<Void> publish(String routingKey, String jsonPayload, OutboxEvent event) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    var metadataBuilder = OutgoingRabbitMQMetadata.builder()
        .withRoutingKey(routingKey)
        .withContentType("application/json");

    if (event.getCorrelationId() != null) {
      metadataBuilder.withCorrelationId(event.getCorrelationId());
    }

    OutgoingRabbitMQMetadata metadata = metadataBuilder
        .withHeader("x-event-id", event.getId().toString())
        .withHeader("x-event-type", event.getEventType())
        .withHeader("x-event-version", String.valueOf(event.getEventVersion()))
        .withHeader("x-aggregate-type", event.getAggregateType())
        .withHeader("x-aggregate-id", event.getAggregateId())
        .withHeader("x-correlation-id", event.getCorrelationId() != null ? event.getCorrelationId() : "")
        .withHeader("x-causation-id", event.getCausationId() != null ? event.getCausationId() : "")
        .withHeader("x-trace-id", event.getTraceId() != null ? event.getTraceId() : "")
        .withHeader("x-span-id", event.getSpanId() != null ? event.getSpanId() : "")
        .withHeader("x-producer", event.getProducer())
        .withHeader("x-actor-id", event.getActorId() != null ? event.getActorId() : "")
        .withHeader("x-tenant-id", event.getTenantId() != null ? event.getTenantId() : "")
        .withHeader("x-occurred-at", event.getOccurredOn().toString())
        .build();

    Message<String> message = Message.of(jsonPayload)
        .addMetadata(metadata)
        .withAck(() -> {
          future.complete(null);
          return CompletableFuture.completedFuture(null);
        })
        .withNack(throwable -> {
          future.completeExceptionally(throwable);
          return CompletableFuture.completedFuture(null);
        });

    emitter.send(message);

    return future;
  }
}
