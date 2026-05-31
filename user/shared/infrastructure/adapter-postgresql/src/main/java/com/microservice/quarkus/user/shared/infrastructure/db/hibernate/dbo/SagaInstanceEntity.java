package com.microservice.quarkus.user.shared.infrastructure.db.hibernate.dbo;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.shared.application.saga.SagaStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "UserSagaInstance")
@Table(name = "user_saga_instances", schema = "quarkus")
@Getter
@Setter
public class SagaInstanceEntity {
  @Id
  private UUID id;

  @Column(name = "saga_type", nullable = false)
  private String sagaType;

  @Column(name = "correlation_id", nullable = false)
  private String correlationId;

  @Column(name = "aggregate_id", nullable = false)
  private String aggregateId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SagaStatus status;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "current_step")
  private String currentStep;
}
