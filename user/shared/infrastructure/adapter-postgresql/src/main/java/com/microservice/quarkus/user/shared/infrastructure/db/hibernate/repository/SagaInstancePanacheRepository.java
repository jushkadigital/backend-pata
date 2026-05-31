package com.microservice.quarkus.user.shared.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.user.shared.infrastructure.db.hibernate.dbo.SagaInstanceEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class SagaInstancePanacheRepository implements PanacheRepositoryBase<SagaInstanceEntity, UUID> {

  public SagaInstanceEntity findByCorrelationId(String correlationId) {
    return find("correlationId", correlationId).firstResult();
  }
}
