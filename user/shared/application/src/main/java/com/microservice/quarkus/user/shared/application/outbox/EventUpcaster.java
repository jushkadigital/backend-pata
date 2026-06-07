package com.microservice.quarkus.user.shared.application.outbox;

import com.microservice.quarkus.user.shared.application.IntegrationEvent;

public interface EventUpcaster {
    String sourceEventType();
    int sourceVersion();
    int targetVersion();
    IntegrationEvent upcast(IntegrationEvent event);
}
