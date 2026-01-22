package com.microservice.quarkus.payment.infrastructure.eventbus;

import com.microservice.quarkus.payment.domain.outbox.OutboxEvent;
import com.microservice.quarkus.payment.domain.outbox.OutboxEventRepository;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.vertx.core.eventbus.EventBus;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Outbox Event Publisher for Payment Bounded Context.
 * Publishes events from subdomains: Products, etc.
 *
 * Priority: 100 (executes AFTER PaymentBootstrap at 40)
 */
@ApplicationScoped
public class OutboxEventPublisher {

    private static final Logger LOG = Logger.getLogger(OutboxEventPublisher.class);

    @Inject
    OutboxEventRepository outboxEventRepository;

    @Inject
    EventBus eventBus;

    void onStart(@Observes @Priority(100) StartupEvent event) {
        LOG.info("Payment Outbox Publisher initialized");
    }

    /**
     * Publishes pending events every 5 seconds
     */
    @Scheduled(every = "5s")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findUnpublished();

        if (pendingEvents.isEmpty()) {
            return;
        }

        LOG.infof("Payment Outbox: %d pending events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = getTopicForEvent(event.getSubdomain(), event.getEventType());
                eventBus.publish(topic, event.getEventPayload());

                event.markAsPublished();
                outboxEventRepository.update(event);

                LOG.debugf("Published: [%s] %s -> %s", event.getSubdomain(), event.getEventType(), topic);

            } catch (Exception e) {
                LOG.errorf("Error publishing event %s [%s]: %s",
                    event.getId(), event.getSubdomain(), e.getMessage());
            }
        }
    }

    /**
     * Cleans published events older than 7 days.
     * Runs daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanOldEvents() {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
        outboxEventRepository.deletePublishedBefore(threshold);
        LOG.infof("Payment Outbox: Cleaned events published before %s", threshold);
    }

    /**
     * Maps subdomain + event type to EventBus topic
     */
    private String getTopicForEvent(String subdomain, String eventType) {
        return switch (subdomain) {
            case "Product" -> switch (eventType) {
                case "ProductCreatedEvent" -> PaymentEventTopics.PRODUCT_CREATED;
                case "ProductUpdatedEvent" -> PaymentEventTopics.PRODUCT_UPDATED;
                default -> {
                    LOG.warnf("Unknown event type for Product: %s", eventType);
                    yield "payment.unknown.event";
                }
            };
            default -> {
                LOG.warnf("Unknown subdomain in Payment: %s", subdomain);
                yield "payment.unknown.event";
            }
        };
    }
}
