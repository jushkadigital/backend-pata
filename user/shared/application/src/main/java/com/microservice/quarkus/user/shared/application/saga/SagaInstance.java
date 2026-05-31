package com.microservice.quarkus.user.shared.application.saga;

import java.time.Instant;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SagaInstance {
  private UUID id;
  private String sagaType;
  private String correlationId;
  private String aggregateId;
  private SagaStatus status;
  private Instant startedAt;
  private Instant completedAt;
  private String currentStep;

  public static SagaInstance start(String sagaType, String correlationId, String aggregateId, String firstStep) {
    return SagaInstance.builder()
        .id(UUID.randomUUID())
        .sagaType(sagaType)
        .correlationId(correlationId)
        .aggregateId(aggregateId)
        .status(SagaStatus.RUNNING)
        .startedAt(Instant.now())
        .completedAt(null)
        .currentStep(firstStep)
        .build();
  }

  public void advanceTo(String step) {
    this.currentStep = step;
  }

  public void complete() {
    this.status = SagaStatus.COMPLETED;
    this.completedAt = Instant.now();
  }

  public void fail(String reason) {
    this.status = SagaStatus.FAILED;
    this.completedAt = Instant.now();
  }

  public void compensate() {
    this.status = SagaStatus.COMPENSATING;
    this.currentStep = "compensate:" + this.currentStep;
  }
}
