package com.microservice.quarkus.user.shared.domain.outbox;

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
  private String subdomain; // "Admin", "Passenger", "IAM"
  private String aggregateType;
  private String aggregateId;
  private String eventType;
  private String eventPayload;
  private Instant occurredOn;
  private Instant publishedAt;
  private Boolean published;
  private EventScope scope;

  public static OutboxEvent create(
      String subdomain,
      String aggregateType,
      String aggregateId,
      String eventType,
      String eventPayload,
      EventScope scope,
      Instant occurredOn) {
    return OutboxEvent.builder()
        .id(UUID.randomUUID())
        .subdomain(subdomain)
        .aggregateType(aggregateType)
        .aggregateId(aggregateId)
        .eventType(eventType)
        .eventPayload(eventPayload)
        .scope(scope)
        .occurredOn(occurredOn)
        .published(false)
        .publishedAt(null)
        .build();
  }

  public void markAsPublished() {
    this.published = true;
    this.publishedAt = Instant.now();
  }
}
