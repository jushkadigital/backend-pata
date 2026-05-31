package com.microservice.quarkus.user.identity.application.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class IdentityMetrics {
    private final Counter registeredCounter;
    private final Counter deletedCounter;

    @Inject
    public IdentityMetrics(MeterRegistry registry) {
        registeredCounter = Counter.builder("identity.user.registered.total")
            .description("Total users registered in identity")
            .register(registry);
        deletedCounter = Counter.builder("identity.user.deleted.total")
            .description("Total users deleted from identity")
            .register(registry);
    }

    // Force eager initialization — ensures counters are registered at startup
    void onStart(@Observes StartupEvent event) {}

    public void recordRegistered() { registeredCounter.increment(); }
    public void recordDeleted() { deletedCounter.increment(); }
}
