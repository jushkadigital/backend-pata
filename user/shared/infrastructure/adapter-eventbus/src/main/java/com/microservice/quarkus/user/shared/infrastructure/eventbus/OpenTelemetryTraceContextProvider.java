package com.microservice.quarkus.user.shared.infrastructure.eventbus;

import com.microservice.quarkus.user.shared.application.outbox.EventMetadata;
import com.microservice.quarkus.user.shared.application.outbox.TraceContextProvider;
import io.opentelemetry.api.trace.Span;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OpenTelemetryTraceContextProvider implements TraceContextProvider {

  @Override
  public EventMetadata current() {
    Span currentSpan = Span.current();
    if (currentSpan == null || !currentSpan.getSpanContext().isValid()) {
      return EventMetadata.empty();
    }
    return new EventMetadata(
        currentSpan.getSpanContext().getTraceId(),
        currentSpan.getSpanContext().getSpanId());
  }
}
