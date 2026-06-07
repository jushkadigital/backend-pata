package com.microservice.quarkus.user.identity.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserSyncRecord(
    String id,
    String email,
    String externalId,
    String userType,      // NEW: PASSENGER or ADMIN
    List<String> roles,   // NOW: only Keycloak composite roles (basic, standard, etc.)
    SyncStatus syncStatus,
    Instant createdAt,
    Instant updatedAt,
    int retryCount,
    int maxRetries,
    Instant nextRetryAt) {

  public UserSyncRecord(String id, String email, String externalId, String userType, List<String> roles, SyncStatus syncStatus, Instant createdAt, Instant updatedAt) {
    this(id, email, externalId, userType, roles, syncStatus, createdAt, updatedAt, 0, 5, null);
  }

  public static UserSyncRecord createNew(String email, String userType, List<String> roles) {
    Instant now = Instant.now();
    return new UserSyncRecord(UUID.randomUUID().toString(), email, null, userType, roles, SyncStatus.PENDING, now, now, 0, 5, null);
  }

  public UserSyncRecord withExternalId(String externalId) {
    return new UserSyncRecord(id, email, externalId, userType, roles, syncStatus, createdAt, Instant.now(), retryCount, maxRetries, nextRetryAt);
  }

  public UserSyncRecord withUserType(String userType) {
    return new UserSyncRecord(id, email, externalId, userType, roles, syncStatus, createdAt, Instant.now(), retryCount, maxRetries, nextRetryAt);
  }

  public UserSyncRecord withSyncStatus(SyncStatus syncStatus) {
    return new UserSyncRecord(id, email, externalId, userType, roles, syncStatus, createdAt, Instant.now(), retryCount, maxRetries, nextRetryAt);
  }

  public UserSyncRecord withRetryCount(int retryCount) {
    return new UserSyncRecord(id, email, externalId, userType, roles, syncStatus, createdAt, Instant.now(), retryCount, maxRetries, nextRetryAt);
  }

  public UserSyncRecord withNextRetryAt(Instant nextRetryAt) {
    return new UserSyncRecord(id, email, externalId, userType, roles, syncStatus, createdAt, Instant.now(), retryCount, maxRetries, nextRetryAt);
  }

  public UserSyncRecord incrementRetry() {
    int newRetryCount = retryCount + 1;
    long backoffSeconds = Math.min((long) Math.pow(2, retryCount), 60);
    Instant newNextRetryAt = Instant.now().plusSeconds(backoffSeconds);
    return new UserSyncRecord(id, email, externalId, userType, roles, syncStatus, createdAt, Instant.now(), newRetryCount, maxRetries, newNextRetryAt);
  }

  public boolean exceededMaxRetries() {
    return retryCount >= maxRetries;
  }

  public boolean isReady() {
    return nextRetryAt == null || Instant.now().isAfter(nextRetryAt);
  }
}
