package com.microservice.quarkus.user.shared.infrastructure.db.hibernate.dbo;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.shared.domain.outbox.EventScope;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "UserOutboxEvent")
@Table(name = "user_outbox_events", schema = "quarkus")
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

  @Enumerated(EnumType.STRING)
  public EventScope scope;

  @Column(name = "published", nullable = false)
  private Boolean published;
}
