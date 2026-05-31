package com.microservice.quarkus.user.shared.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.user.shared.application.saga.SagaInstance;
import com.microservice.quarkus.user.shared.application.saga.SagaRepository;
import com.microservice.quarkus.user.shared.infrastructure.db.hibernate.dbo.SagaInstanceEntity;
import com.microservice.quarkus.user.shared.infrastructure.db.hibernate.mapper.SagaInstanceMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SagaDboRepository implements SagaRepository {

  @Inject
  SagaInstancePanacheRepository panacheRepository;

  @Inject
  SagaInstanceMapper mapper;

  @Override
  @Transactional
  public void save(SagaInstance saga) {
    SagaInstanceEntity entity = mapper.toEntity(saga);
    panacheRepository.persistAndFlush(entity);
  }

  @Override
  @Transactional
  public void update(SagaInstance saga) {
    SagaInstanceEntity entity = panacheRepository.findById(saga.getId());
    if (entity != null) {
      mapper.updateEntityFromDomain(saga, entity);
      panacheRepository.persist(entity);
    }
  }

  @Override
  public Optional<SagaInstance> findById(UUID id) {
    SagaInstanceEntity entity = panacheRepository.findById(id);
    return Optional.ofNullable(entity).map(mapper::toDomain);
  }

  @Override
  public Optional<SagaInstance> findByCorrelationId(String correlationId) {
    SagaInstanceEntity entity = panacheRepository.findByCorrelationId(correlationId);
    return Optional.ofNullable(entity).map(mapper::toDomain);
  }
}
