package com.microservice.quarkus.user.identity.application.api;

import com.microservice.quarkus.user.identity.application.dto.RoleSyncRecord;
import java.util.List;

public interface RoleSyncRepository {
  RoleSyncRecord findByName(String name);
  List<RoleSyncRecord> findByClientId(String clientId);
  RoleSyncRecord save(RoleSyncRecord record);
  List<RoleSyncRecord> findAll();
  void deleteAll();
}
