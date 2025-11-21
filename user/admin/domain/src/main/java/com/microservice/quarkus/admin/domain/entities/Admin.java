package com.microservice.quarkus.admin.domain.entities;

import java.time.Instant;
import java.util.List;

import com.microservice.quarkus.admin.domain.shared.Entity;
import com.microservice.quarkus.admin.domain.shared.RootAggregate;

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
  private Instant createdAt;
  private Instant updatedAt;

  public static Admin createNew(AdminId id, EmailAddress email, AdminType type) {
    Instant now = Instant.now();
    return Admin.builder()
        .id(id)
        .email(email)
        .type(type)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  @Override
  public boolean sameIdentityAs(Admin other) {
    return other != null && this.id.equals(other.getId());
  }
}
