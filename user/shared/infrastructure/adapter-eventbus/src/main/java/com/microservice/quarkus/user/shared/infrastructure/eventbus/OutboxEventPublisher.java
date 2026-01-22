package com.microservice.quarkus.user.shared.infrastructure.eventbus;

import com.microservice.quarkus.user.shared.domain.outbox.EventScope;
import com.microservice.quarkus.user.shared.domain.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.domain.outbox.OutboxEventRepository;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Consolidated Outbox Event Publisher for User Bounded Context
 * Publishes events from all subdomains: IAM, Admin, Passenger
 *
 * Priority: 100 (executes AFTER all subdomain bootstraps)
 */
@ApplicationScoped
public class OutboxEventPublisher {

  @Inject
  OutboxEventRepository outboxEventRepository;

  @Inject
  EventBus eventBus;

  @Inject
  RabbitMqBroker rabbitMqBroker;

  /**
   * Initializes after all subdomain bootstraps complete
   * Priority 100 ensures execution after:
   * - IamBootstrap (Priority 10)
   * - PassengerBootstrap (Priority 20)
   * - AdminBootstrap (Priority 30)
   */
  void onStart(@Observes @Priority(100) StartupEvent event) {
    System.out.println("📤 Outbox Publisher initialized for User Bounded Context");
    System.out.println("   Publishing events from subdomains: IAM, Admin, Passenger");
  }

  /**
   * Publica eventos pendientes cada 5 segundos
   */
  @Scheduled(every = "5s")
  @Transactional
  public void publishPendingEvents() {
    List<OutboxEvent> pendingEvents = outboxEventRepository.findUnpublished();

    if (pendingEvents.isEmpty()) {
      return;
    }

    System.out.println("📤 Outbox Publisher: " + pendingEvents.size() + " eventos pendientes");

    for (OutboxEvent event : pendingEvents) {
      try {
        // Publicar al EventBus
        String topic = getTopicForEvent(event.getSubdomain(), event.getEventType());

        if (event.getScope() == EventScope.INTERNAL_ONLY || event.getScope() == EventScope.BOTH) {
          eventBus.publish(topic, event.getEventPayload());
          System.out.println("🏠 Internal Bus: " + topic);
        }

        if (event.getScope() == EventScope.EXTERNAL_ONLY || event.getScope() == EventScope.BOTH) {
          // Bloqueamos esperando a RabbitMQ para asegurar consistencia
          rabbitMqBroker.publish(topic, event.getEventPayload()).toCompletableFuture().join();
          System.out.println("🌍 RabbitMQ: " + topic);
        }

        // Marcar como publicado
        event.markAsPublished();
        outboxEventRepository.update(event);

        System.out.println("✅ Publicado: [" + event.getSubdomain() + "] " +
            event.getEventType() + " → " + topic);

      } catch (Exception e) {
        System.err.println("❌ Error publicando evento " + event.getId() +
            " [" + event.getSubdomain() + "]: " + e.getMessage());
        // Se reintentará en el siguiente ciclo
      }
    }
  }

  /**
   * Limpia eventos publicados hace más de 7 días
   * Se ejecuta diariamente a las 2 AM
   */
  @Scheduled(cron = "0 0 2 * * ?")
  @Transactional
  public void cleanOldEvents() {
    Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
    outboxEventRepository.deletePublishedBefore(threshold);
    System.out.println("🧹 Outbox: Eventos publicados antes de " + threshold + " eliminados");
  }

  /**
   * Mapea el subdomain + tipo de evento al topic del EventBus
   */
  private String getTopicForEvent(String subdomain, String eventType) {
    return switch (subdomain) {
      case "Admin" -> switch (eventType) {
        case "AdminRegisteredEvent" -> "admin.registered";
        default -> {
          System.err.println("⚠️ Tipo de evento desconocido para Admin: " + eventType);
          yield "unknown.event";
        }
      };
      case "Passenger" -> switch (eventType) {
        case "PassengerRegisteredEvent" -> "passenger.registered";
        default -> {
          System.err.println("⚠️ Tipo de evento desconocido para Passenger: " + eventType);
          yield "unknown.event";
        }
      };
      case "IAM" -> switch (eventType) {
        case "UserRegisteredEvent" -> "iam.user.registered";
        default -> {
          System.err.println("⚠️ Tipo de evento desconocido para IAM: " + eventType);
          yield "unknown.event";
        }
      };
      default -> {
        System.err.println("⚠️ Subdomain desconocido: " + subdomain);
        yield "unknown.event";
      }
    };
  }
}
