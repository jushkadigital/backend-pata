package com.microservice.quarkus.user.identity.application.api;

import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import com.microservice.quarkus.user.identity.application.dto.UserSyncRecord;
import java.util.List;

public interface IdentitySyncRepository {
  UserSyncRecord findByEmail(String email);
  UserSyncRecord findByExternalId(String externalId);
  UserSyncRecord save(UserSyncRecord record);
  List<UserSyncRecord> findAll();
  List<UserSyncRecord> findBySyncStatus(SyncStatus status);
  void deleteAll();
  void deleteByExternalId(String externalId);
}
