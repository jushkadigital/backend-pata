package com.microservice.quarkus.user.shared.application.outbox;

import java.time.Instant;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OutboxEvent {
  private UUID id;
  private String eventType;
  private Integer eventVersion;
  private String aggregateType;
  private String aggregateId;
  private String correlationId;
  private String causationId;
  private String traceId;
  private String spanId;
  private String producer;
  private String actorId;
  private String tenantId;
  private String eventPayload;
  private Instant occurredOn;
  private Instant publishedAt;
  private Boolean published;
  private EventScope scope;

  private Integer retryCount;
  private Integer maxRetries;
  private Instant nextRetryAt;

  private Boolean dead;
  private String deadReason;
  private Instant deadAt;

  public static OutboxEvent create(
      String eventType,
      Integer eventVersion,
      String aggregateType,
      String aggregateId,
      String correlationId,
      String causationId,
      String traceId,
      String spanId,
      String producer,
      String actorId,
      String tenantId,
      String eventPayload,
      EventScope scope,
      Instant occurredOn) {
    return OutboxEvent.builder()
        .id(UUID.randomUUID())
        .eventType(eventType)
        .eventVersion(eventVersion)
        .aggregateType(aggregateType)
        .aggregateId(aggregateId)
        .correlationId(correlationId != null ? correlationId : UUID.randomUUID().toString())
        .causationId(causationId)
        .traceId(traceId)
        .spanId(spanId)
        .producer(producer)
        .actorId(actorId)
        .tenantId(tenantId)
        .eventPayload(eventPayload)
        .scope(scope)
        .occurredOn(occurredOn)
        .published(false)
        .publishedAt(null)
        .retryCount(0)
        .maxRetries(5)
        .nextRetryAt(null)
        .dead(false)
        .deadReason(null)
        .deadAt(null)
        .build();
  }

  public void markAsPublished() {
    this.published = true;
    this.publishedAt = Instant.now();
  }

  public void incrementRetry() {
    this.retryCount = this.retryCount + 1;
    long delayMs = (long) Math.pow(2, this.retryCount) * 1000;
    this.nextRetryAt = Instant.now().plusMillis(delayMs);
  }

  public boolean shouldRetry() {
    return !this.dead && this.retryCount < this.maxRetries;
  }

  public boolean isReadyForRetry() {
    return !this.dead && this.nextRetryAt != null && Instant.now().isAfter(this.nextRetryAt);
  }

  public void markAsDead(String reason) {
    this.dead = true;
    this.deadReason = reason;
    this.deadAt = Instant.now();
  }

  public boolean isNotReady() {
    if (this.dead) return true;
    if (this.published) return true;
    if (this.nextRetryAt != null && Instant.now().isBefore(this.nextRetryAt)) return true;
    return false;
  }
}
