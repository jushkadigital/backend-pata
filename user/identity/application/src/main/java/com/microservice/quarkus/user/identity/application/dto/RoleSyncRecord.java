package com.microservice.quarkus.user.identity.application.dto;

import java.time.Instant;
import java.util.UUID;

public record RoleSyncRecord(
    String id,
    String name,
    String description,
    String clientId,
    SyncStatus syncStatus,
    Instant createdAt,
    Instant updatedAt) {

  public static RoleSyncRecord createNew(String name, String description, String clientId) {
    Instant now = Instant.now();
    return new RoleSyncRecord(UUID.randomUUID().toString(), name, description, clientId, SyncStatus.PENDING, now, now);
  }

  public RoleSyncRecord withSyncStatus(SyncStatus syncStatus) {
    return new RoleSyncRecord(id, name, description, clientId, syncStatus, createdAt, Instant.now());
  }
}
