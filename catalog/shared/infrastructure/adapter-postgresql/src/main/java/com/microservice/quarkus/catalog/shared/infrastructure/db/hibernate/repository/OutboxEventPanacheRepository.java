package com.microservice.quarkus.catalog.shared.infrastructure.db.hibernate.repository;

import com.microservice.quarkus.catalog.shared.infrastructure.db.hibernate.dbo.OutboxEventEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OutboxEventPanacheRepository implements PanacheRepositoryBase<OutboxEventEntity, UUID> {

    public List<OutboxEventEntity> findUnpublished() {
        return list("published = false ORDER BY occurredOn ASC");
    }

    public List<OutboxEventEntity> findUnpublishedBySubdomain(String subdomain) {
        return list("subdomain = ?1 AND published = false ORDER BY occurredOn ASC", subdomain);
    }

    public long deletePublishedBefore(Instant threshold) {
        return delete("published = true AND publishedAt < ?1", threshold);
    }
}
