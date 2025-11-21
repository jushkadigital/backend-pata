package com.microservice.quarkus.user.iam.domain;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.AccessLevel;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {
  private UserId id;
  private EmailAddress email;
  private String externalId; // ID de Keycloak
  private UserType type; // passenger o admin
  private SyncStatus syncStatus; // estado de sincronizacion
  private Instant createdAt;
  private Instant updatedAt;

  public static User createNew(UserId id, EmailAddress email, UserType type) {
    Instant now = Instant.now();

    return User.builder()
        .id(id)
        .email(email)
        .type(type)
        .syncStatus(SyncStatus.PENDING)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
