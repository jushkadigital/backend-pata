package com.microservice.quarkus.user.passenger.application.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class PassengerMetrics {
    private final Counter createdCounter;
    private final Counter deletedCounter;

    @Inject
    public PassengerMetrics(MeterRegistry registry) {
        createdCounter = Counter.builder("passenger.created.total")
            .description("Total passengers created")
            .register(registry);
        deletedCounter = Counter.builder("passenger.deleted.total")
            .description("Total passengers deleted")
            .register(registry);
    }

    void onStart(@Observes StartupEvent event) {
        // Force eager initialization — ensures counters are registered at startup
    }

    public void recordCreated() { createdCounter.increment(); }
    public void recordDeleted() { deletedCounter.increment(); }
}
