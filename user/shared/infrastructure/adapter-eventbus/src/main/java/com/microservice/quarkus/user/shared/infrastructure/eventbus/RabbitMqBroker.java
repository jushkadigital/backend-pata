package com.microservice.quarkus.user.shared.infrastructure.eventbus;

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
  @Channel("domain-events-out") // Debe coincidir con el application.yml
  Emitter<String> emitter;

  /**
   * Publica un mensaje en RabbitMQ con una routing key dinámica.
   * Retorna un futuro para poder esperar el ACK (confirmación).
   *
   * @param routingKey  El tópico, ej: "iam.user.registered"
   * @param jsonPayload El contenido del evento ya serializado en JSON
   */
  public CompletionStage<Void> publish(String routingKey, String jsonPayload) {
    // 1. Preparamos el "monitor" de la operación
    CompletableFuture<Void> future = new CompletableFuture<>();

    // 2. Construimos la metadata
    OutgoingRabbitMQMetadata metadata = OutgoingRabbitMQMetadata.builder()
        .withRoutingKey(routingKey)
        .withContentType("application/json")
        .build();

    // 3. Creamos el mensaje conectando el ACK/NACK al Future
    Message<String> message = Message.of(jsonPayload)
        .addMetadata(metadata)
        .withAck(() -> {
          // Si RabbitMQ confirma recepción -> Completamos el futuro
          future.complete(null);
          return CompletableFuture.completedFuture(null);
        })
        .withNack(throwable -> {
          // Si falla -> Completamos el futuro con error
          future.completeExceptionally(throwable);
          return CompletableFuture.completedFuture(null);
        });

    // 4. Enviamos (Este método retorna void)
    emitter.send(message);

    // 5. Retornamos el futuro para que tu Outbox pueda esperar (join)
    return future;

  }
}
