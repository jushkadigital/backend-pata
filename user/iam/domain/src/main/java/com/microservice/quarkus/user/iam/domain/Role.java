package com.microservice.quarkus.user.iam.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Role {
  private String id;
  private String name;
  private String description;
  private SyncStatus syncStatus; // estado de sincronizacion
  private String clientId;
  private Instant createdAt;
  private Instant updatedAt;

  public static Role createNew(String id, String name, String description, String clientId) {

    Instant now = Instant.now();
    return Role.builder()
        .id(id)
        .name(name)
        .description(description)
        .clientId(clientId)
        .syncStatus(SyncStatus.PENDING)
        .createdAt(now).updatedAt(now).build();

  }
}
