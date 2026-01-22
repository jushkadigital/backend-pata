package com.microservice.quarkus.user.admin.domain.entities;

import java.time.Instant;
import java.util.List;

import com.microservice.quarkus.user.admin.domain.events.AdminRegisteredEvent;
import com.microservice.quarkus.user.admin.domain.shared.Entity;
import com.microservice.quarkus.user.admin.domain.shared.RootAggregate;

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
public class Admin extends RootAggregate implements Entity<Admin> {
  private AdminId id;
  private AdminType type;
  private EmailAddress email;
  private String externalId;
  private Instant createdAt;
  private Instant updatedAt;

  public static Admin createNew(String externalId, String email, String type) {
    Instant now = Instant.now();
    Admin admin = Admin.builder()
        .id(AdminId.random())
        .externalId(externalId)
        .email(new EmailAddress(email))
        .type(AdminType.valueOf(type.toUpperCase()))
        .createdAt(now)
        .updatedAt(now)
        .build();

    // Registrar evento de dominio
    admin.registerEvent(new AdminRegisteredEvent(externalId, email, type));

    return admin;
  }

  @Override
  public boolean sameIdentityAs(Admin other) {
    return other != null && this.id.equals(other.getId());
  }
}
