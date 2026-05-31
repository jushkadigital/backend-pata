package com.microservice.quarkus.user.shared.application.outbox;

public record EventMetadata(
    String traceId,
    String spanId) {

  public static EventMetadata empty() {
    return new EventMetadata(null, null);
  }
}
