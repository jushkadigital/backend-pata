package com.microservice.quarkus.user.identity.infrastructure.db.hibernate.entity;

import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserType;
import com.microservice.quarkus.user.identity.infrastructure.db.hibernate.converter.EmailAddressConverter;
import com.microservice.quarkus.user.shared.domain.EmailAddress;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users", schema = "quarkus")
@Getter
@Setter
public class UserSyncEntity extends PanacheEntityBase {
  @Id
  private String id;

  @Convert(converter = EmailAddressConverter.class)
  @Column(name = "email", nullable = false)
  private EmailAddress email;

  @Column(name = "external_id", unique = true)
  private String externalId;

  @Enumerated(EnumType.STRING)
  @Column(name = "user_type", nullable = false)
  private UserType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "sync_status", nullable = false)
  private SyncStatus syncStatus;

  @Column(name = "created_at", nullable = false)
  private java.time.Instant createdAt;

  @Column(name = "updated_at")
  private java.time.Instant updatedAt;

  @Column(name = "retry_count")
  private int retryCount;

  @Column(name = "max_retries")
  private int maxRetries;

  @Column(name = "next_retry_at")
  private java.time.Instant nextRetryAt;
}
