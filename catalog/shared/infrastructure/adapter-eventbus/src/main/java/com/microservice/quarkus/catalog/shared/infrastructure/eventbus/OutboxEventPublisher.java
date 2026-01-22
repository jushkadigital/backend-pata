package com.microservice.quarkus.catalog.shared.infrastructure.eventbus;

import com.microservice.quarkus.catalog.shared.domain.outbox.OutboxEvent;
import com.microservice.quarkus.catalog.shared.domain.outbox.OutboxEventRepository;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.vertx.core.eventbus.EventBus;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Consolidated Outbox Event Publisher for Catalog Bounded Context
 * Publishes events from all subdomains: Services, Products, etc.
 *
 * Priority: 100 (executes AFTER all subdomain bootstraps)
 */
@ApplicationScoped
public class OutboxEventPublisher {

    @Inject
    OutboxEventRepository outboxEventRepository;

    @Inject
    EventBus eventBus;

    /**
     * Initializes after all subdomain bootstraps complete
     * Priority 100 ensures execution after:
     * - ServicesBootstrap (Priority 40)
     * - ToursBootstrap (Priority 50)
     * - Future subdomain bootstraps
     */
    void onStart(@Observes @Priority(100) StartupEvent event) {
        System.out.println("📤 Catalog Outbox Publisher initialized");
        System.out.println("   Publishing events from subdomains: Services, Tours");
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

        System.out.println("📤 Catalog Outbox: " + pendingEvents.size() + " eventos pendientes");

        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = getTopicForEvent(event.getSubdomain(), event.getEventType());
                eventBus.publish(topic, event.getEventPayload());

                event.markAsPublished();
                outboxEventRepository.update(event);

                System.out.println("✅ Publicado: [" + event.getSubdomain() + "] " +
                        event.getEventType() + " → " + topic);

            } catch (Exception e) {
                System.err.println("❌ Error publicando evento " + event.getId() +
                        " [" + event.getSubdomain() + "]: " + e.getMessage());
            }
        }
    }

    /**
     * Limpia eventos publicados hace más de 7 días
     * Se ejecuta diariamente a las 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanOldEvents() {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
        outboxEventRepository.deletePublishedBefore(threshold);
        System.out.println("🧹 Catalog Outbox: Eventos publicados antes de " + threshold + " eliminados");
    }

    /**
     * Mapea el subdomain + tipo de evento al topic del EventBus
     */
    private String getTopicForEvent(String subdomain, String eventType) {
        return switch (subdomain) {
            case "Services" -> switch (eventType) {
                case "ServiceCreatedEvent" -> "catalog.service.created";
                case "ServicePublishedEvent" -> "catalog.service.published";
                case "ServiceUpgradedEvent" -> "catalog.service.upgraded";
                case "ServiceDeprecatedEvent" -> "catalog.service.deprecated";
                case "ServiceArchivedEvent" -> "catalog.service.archived";
                default -> {
                    System.err.println("⚠️ Tipo de evento desconocido para Services: " + eventType);
                    yield "catalog.unknown.event";
                }
            };
            case "Tours" -> switch (eventType) {
                case "TourCreatedEvent" -> "catalog.tour.created";
                case "TourUpdatedEvent" -> "catalog.tour.updated";
                case "TourPublishedEvent" -> "catalog.tour.published";
                case "TourSuspendedEvent" -> "catalog.tour.suspended";
                case "TourDiscontinuedEvent" -> "catalog.tour.discontinued";
                case "ServiceAddedToTourEvent" -> "catalog.tour.service-added";
                case "CombinationPriceUpdatedEvent" -> "catalog.tour.combination-price-updated";
                default -> {
                    System.err.println("⚠️ Tipo de evento desconocido para Tours: " + eventType);
                    yield "catalog.unknown.event";
                }
            };
            default -> {
                System.err.println("⚠️ Subdomain desconocido en Catalog: " + subdomain);
                yield "catalog.unknown.event";
            }
        };
    }
}
