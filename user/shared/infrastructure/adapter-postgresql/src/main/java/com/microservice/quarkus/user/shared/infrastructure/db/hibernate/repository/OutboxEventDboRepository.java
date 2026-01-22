package com.microservice.quarkus.user.shared.infrastructure.db.hibernate.repository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.microservice.quarkus.user.shared.domain.outbox.OutboxEvent;
import com.microservice.quarkus.user.shared.domain.outbox.OutboxEventRepository;
import com.microservice.quarkus.user.shared.infrastructure.db.hibernate.dbo.OutboxEventEntity;
import com.microservice.quarkus.user.shared.infrastructure.db.hibernate.mapper.OutboxEventMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OutboxEventDboRepository implements OutboxEventRepository {

  @Inject
  OutboxEventPanacheRepository repository;

  @Inject
  OutboxEventMapper mapper;

  @Override
  @Transactional
  public void save(OutboxEvent event) {
    OutboxEventEntity entity = mapper.toDbo(event);
    repository.persistAndFlush(entity);
    mapper.updateDomainFromEntity(entity, event);
  }

  @Override
  @Transactional
  public void update(OutboxEvent event) {
    OutboxEventEntity entity = repository.findByIdOptional(event.getId())
        .orElseThrow(() -> new RuntimeException("OutboxEvent not found: " + event.getId()));
    mapper.updateEntityFromDomain(event, entity);
    repository.persist(entity);
    mapper.updateDomainFromEntity(entity, event);
  }

  @Override
  public List<OutboxEvent> findUnpublished() {
    return repository.findUnpublished().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<OutboxEvent> findUnpublishedBySubdomain(String subdomain) {
    return repository.findUnpublishedBySubdomain(subdomain).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void deletePublishedBefore(Instant threshold) {
    repository.deletePublishedBefore(threshold);
  }
}
