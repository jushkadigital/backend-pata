package com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo;

import java.time.Instant;
import java.time.LocalDateTime;

import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.SyncStatus;
import com.microservice.quarkus.user.iam.domain.UserId;
import com.microservice.quarkus.user.iam.domain.UserType;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo.converter.EmailAddressConverter;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo.converter.UserIdConverter;

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
public class UserEntity {
  @Id
  private String id;
  @Convert(converter = EmailAddressConverter.class)
  @Column(name = "email", unique = true, nullable = false)
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
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

}
