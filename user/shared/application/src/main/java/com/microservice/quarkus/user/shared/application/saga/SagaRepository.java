package com.microservice.quarkus.user.shared.application.saga;

import java.util.Optional;
import java.util.UUID;

public interface SagaRepository {
  void save(SagaInstance saga);
  void update(SagaInstance saga);
  Optional<SagaInstance> findById(UUID id);
  Optional<SagaInstance> findByCorrelationId(String correlationId);
}
