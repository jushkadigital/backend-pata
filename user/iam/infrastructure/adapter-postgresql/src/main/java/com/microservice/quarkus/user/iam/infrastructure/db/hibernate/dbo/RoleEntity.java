package com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo;

import java.time.Instant;
import java.time.LocalDateTime;

import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.SyncStatus;
import com.microservice.quarkus.user.iam.domain.UserType;
import com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo.converter.EmailAddressConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles", schema = "quarkus", indexes = {
    @Index(name = "idx_roles_client_id", columnList = "client_id")
})
@Getter
@Setter
public class RoleEntity {
  @Id
  private String id;

  @Column(name = "name", unique = true, nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "sync_status", nullable = false)
  private SyncStatus syncStatus;

  @Column(name = "client_id", nullable = false)
  private String clientId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

}
