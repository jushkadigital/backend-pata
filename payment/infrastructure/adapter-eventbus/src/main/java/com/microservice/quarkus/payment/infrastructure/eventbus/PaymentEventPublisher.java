package com.microservice.quarkus.payment.infrastructure.eventbus;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PaymentEventPublisher {

    private static final Logger LOG = Logger.getLogger(PaymentEventPublisher.class);

    private final EventBus eventBus;

    @Inject
    public PaymentEventPublisher(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void publishProductCreated(String productId, String productName) {
        LOG.debugf("Publishing product created event: %s", productId);
        JsonObject payload = new JsonObject()
                .put("productId", productId)
                .put("productName", productName);
        eventBus.publish(PaymentEventTopics.PRODUCT_CREATED, payload.encode());
    }

    public void publishProductUpdated(String productId) {
        LOG.debugf("Publishing product updated event: %s", productId);
        eventBus.publish(PaymentEventTopics.PRODUCT_UPDATED, productId);
    }
}
