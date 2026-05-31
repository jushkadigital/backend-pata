package com.microservice.quarkus.user.identity.application.service;

import com.microservice.quarkus.user.identity.application.api.RoleIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.RoleSyncRepository;
import com.microservice.quarkus.user.identity.application.dto.KeycloakRoleDTO;
import com.microservice.quarkus.user.identity.application.dto.RoleSyncRecord;
import com.microservice.quarkus.user.identity.application.dto.SyncStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.util.List;

@ApplicationScoped
public class RoleService {

  private final RoleIdentityProvider keycloakClient;
  private final RoleSyncRepository syncRepository;

  public RoleService(RoleIdentityProvider keycloakClient, RoleSyncRepository syncRepository) {
    this.keycloakClient = keycloakClient;
    this.syncRepository = syncRepository;
  }

  @WithSpan("role.create")
  @Transactional
  public String register(String name, String description, String clientId) {
    RoleSyncRecord existing = syncRepository.findByName(name);
    if (existing != null && existing.syncStatus() == SyncStatus.SYNCED) {
      return existing.name();
    }

    RoleSyncRecord record = RoleSyncRecord.createNew(name, description, clientId);
    record = syncRepository.save(record);

    try {
      String externalId = keycloakClient.createRole(name, description, clientId);
      if (externalId == null || externalId.isEmpty()) {
        throw new IllegalArgumentException("El rol ya existe en keycloakClient: " + name);
      }
      record = record.withSyncStatus(SyncStatus.SYNCED);
      syncRepository.save(record);
    } catch (Exception e) {
      record = record.withSyncStatus(SyncStatus.FAILED);
      syncRepository.save(record);
    }

    return record.name();
  }

  @WithSpan("role.findAll")
  public List<KeycloakRoleDTO> allRolesKeycloak() {
    return keycloakClient.getAllRoles();
  }
}
