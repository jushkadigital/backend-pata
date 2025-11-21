package com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo.converter.EmailAddressConverter;
import com.microservice.quarkus.admin.domain.entities.AdminType;
import com.microservice.quarkus.admin.domain.entities.EmailAddress;
import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo.converter.AdminIdConverter;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admins", schema = "quarkus")
@Getter
@Setter
public class AdminEntity {
  @Id
  private String id;
  @Convert(converter = EmailAddressConverter.class)
  @Column(name = "email", nullable = false)
  private EmailAddress email;

  @Enumerated(EnumType.STRING)
  @Column(name = "user_type", nullable = false)
  private AdminType type;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

}
