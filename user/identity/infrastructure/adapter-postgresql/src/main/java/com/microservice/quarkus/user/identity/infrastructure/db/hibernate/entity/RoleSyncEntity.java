package com.microservice.quarkus.user.identity.infrastructure.db.hibernate.entity;

import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.infrastructure.db.hibernate.converter.EmailAddressConverter;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
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
public class RoleSyncEntity extends PanacheEntityBase {
  @Id
  private String id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "sync_status", nullable = false)
  private SyncStatus syncStatus;

  @Column(name = "client_id", nullable = false)
  private String clientId;

  @Column(name = "created_at", nullable = false)
  private java.time.Instant createdAt;

  @Column(name = "updated_at")
  private java.time.Instant updatedAt;
}
