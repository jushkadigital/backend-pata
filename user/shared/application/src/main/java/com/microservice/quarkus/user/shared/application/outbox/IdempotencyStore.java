package com.microservice.quarkus.user.shared.application.outbox;

public interface IdempotencyStore {
    /**
     * Attempts to acquire processing rights for the given eventId and consumerGroup.
     * Returns true if this is a new event (should process), false if duplicate (skip).
     */
    boolean tryAcquire(String eventId, String consumerGroup);
}
