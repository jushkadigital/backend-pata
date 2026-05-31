package com.microservice.quarkus.user.shared.application.saga;

public interface SagaMetrics {
  void recordStarted();
  void recordCompleted();
  void recordFailed();
}
