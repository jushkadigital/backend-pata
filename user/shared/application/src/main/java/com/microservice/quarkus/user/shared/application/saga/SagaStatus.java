package com.microservice.quarkus.user.shared.application.saga;

public enum SagaStatus {
  RUNNING,
  COMPLETED,
  FAILED,
  COMPENSATING
}
