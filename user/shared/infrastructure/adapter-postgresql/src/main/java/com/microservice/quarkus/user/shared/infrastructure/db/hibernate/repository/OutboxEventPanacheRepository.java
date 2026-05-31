package com.microservice.quarkus.user.shared.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.user.shared.infrastructure.db.hibernate.dbo.OutboxEventEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OutboxEventPanacheRepository implements PanacheRepositoryBase<OutboxEventEntity, UUID> {

  public List<OutboxEventEntity> findUnpublished() {
    return list("published = false AND dead = false ORDER BY occurredOn ASC");
  }

  public List<OutboxEventEntity> findDeadEvents() {
    return list("dead = true ORDER BY deadAt ASC");
  }

  public long deletePublishedBefore(Instant threshold) {
    return delete("published = true AND publishedAt < ?1", threshold);
  }
}
