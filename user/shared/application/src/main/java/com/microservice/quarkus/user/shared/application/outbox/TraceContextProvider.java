package com.microservice.quarkus.user.shared.application.outbox;

public interface TraceContextProvider {
  EventMetadata current();
}
