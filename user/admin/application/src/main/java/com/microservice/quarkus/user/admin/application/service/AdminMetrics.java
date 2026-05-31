package com.microservice.quarkus.user.admin.application.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class AdminMetrics {
    private final Counter createdCounter;
    private final Counter deletedCounter;

    @Inject
    public AdminMetrics(MeterRegistry registry) {
        createdCounter = Counter.builder("admin.created.total")
            .description("Total admins created")
            .register(registry);
        deletedCounter = Counter.builder("admin.deleted.total")
            .description("Total admins deleted")
            .register(registry);
    }

    // Force eager initialization — ensures counters are registered at startup
    void onStart(@Observes StartupEvent event) {}

    public void recordCreated() { createdCounter.increment(); }
    public void recordDeleted() { deletedCounter.increment(); }
}
