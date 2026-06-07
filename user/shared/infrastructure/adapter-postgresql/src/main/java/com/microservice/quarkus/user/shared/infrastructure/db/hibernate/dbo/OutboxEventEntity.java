package com.microservice.quarkus.user.shared.infrastructure.db.hibernate.dbo;

import java.time.Instant;
import java.util.UUID;

import com.microservice.quarkus.user.shared.application.outbox.EventScope;

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

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "event_version", nullable = false)
  private Integer eventVersion;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private String aggregateId;

  @Column(name = "correlation_id")
  private String correlationId;

  @Column(name = "causation_id")
  private String causationId;

  @Column(name = "trace_id")
  private String traceId;

  @Column(name = "span_id")
  private String spanId;

  @Column(name = "producer", nullable = false, length = 100)
  private String producer;

  @Column(name = "actor_id")
  private String actorId;

  @Column(name = "tenant_id")
  private String tenantId;

  @Column(name = "spec_version", length = 50)
  private String specVersion;

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

  @Column(name = "retry_count", nullable = false)
  private Integer retryCount;

  @Column(name = "max_retries", nullable = false)
  private Integer maxRetries;

  @Column(name = "next_retry_at")
  private Instant nextRetryAt;

  @Column(name = "dead", nullable = false)
  private Boolean dead;

  @Column(name = "dead_reason", columnDefinition = "TEXT")
  private String deadReason;

  @Column(name = "dead_at")
  private Instant deadAt;
}
