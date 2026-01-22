package com.microservice.quarkus.catalog.shared.infrastructure.db.hibernate.dbo;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "CatalogOutboxEvent")
@Table(name = "catalog_outbox_events", schema = "quarkus", indexes = {
    @Index(name = "idx_catalog_outbox_published", columnList = "published"),
    @Index(name = "idx_catalog_outbox_subdomain", columnList = "subdomain")
})
@Getter
@Setter
public class OutboxEventEntity {

  @Id
  private UUID id;

  @Column(name = "subdomain", nullable = false, length = 50)
  private String subdomain;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private String aggregateId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "event_payload", nullable = false, columnDefinition = "TEXT")
  private String eventPayload;

  @Column(name = "occurred_on", nullable = false)
  private Instant occurredOn;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "published", nullable = false)
  private Boolean published;
}
