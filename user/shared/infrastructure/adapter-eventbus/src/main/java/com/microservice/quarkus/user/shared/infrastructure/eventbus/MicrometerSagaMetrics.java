package com.microservice.quarkus.user.shared.infrastructure.eventbus;

import com.microservice.quarkus.user.shared.application.saga.SagaMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MicrometerSagaMetrics implements SagaMetrics {

  private final Counter startedCounter;
  private final Counter completedCounter;
  private final Counter failedCounter;

  @Inject
  public MicrometerSagaMetrics(MeterRegistry meterRegistry) {
    this.startedCounter = Counter.builder("saga.started.total")
        .description("Total sagas started")
        .register(meterRegistry);
    this.completedCounter = Counter.builder("saga.completed.total")
        .description("Total sagas completed")
        .register(meterRegistry);
    this.failedCounter = Counter.builder("saga.failed.total")
        .description("Total sagas failed")
        .register(meterRegistry);
  }

  @Override
  public void recordStarted() {
    startedCounter.increment();
  }

  @Override
  public void recordCompleted() {
    completedCounter.increment();
  }

  @Override
  public void recordFailed() {
    failedCounter.increment();
  }
}
