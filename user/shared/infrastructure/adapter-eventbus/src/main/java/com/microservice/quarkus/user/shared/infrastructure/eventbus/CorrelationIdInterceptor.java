package com.microservice.quarkus.user.shared.infrastructure.eventbus;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

@ApplicationScoped
public class CorrelationIdInterceptor {

  private static final Logger log = LoggerFactory.getLogger(CorrelationIdInterceptor.class);
  private static final String CORRELATION_ID_HEADER = "x-correlation-id";

  public String getOrCreateCorrelationId() {
    String existing = MDC.get("correlationId");
    if (existing != null && !existing.isBlank()) {
      return existing;
    }
    String newId = UUID.randomUUID().toString();
    MDC.put("correlationId", newId);
    return newId;
  }

  public void setCorrelationId(String correlationId) {
    if (correlationId != null && !correlationId.isBlank()) {
      MDC.put("correlationId", correlationId);
    }
  }

  public void clear() {
    MDC.remove("correlationId");
  }
}
