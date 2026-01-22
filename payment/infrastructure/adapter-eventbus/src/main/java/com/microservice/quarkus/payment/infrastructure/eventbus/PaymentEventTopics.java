package com.microservice.quarkus.payment.infrastructure.eventbus;

/**
 * Event bus topic constants for payment module.
 */
public final class PaymentEventTopics {

    private PaymentEventTopics() {}

    // Payment internal events
    public static final String PRODUCT_CREATED = "payment.product.created";
    public static final String PRODUCT_UPDATED = "payment.product.updated";
}
