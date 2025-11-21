package com.microservice.quarkus.user.iam.application.service;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.domain.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoleService {

  @Inject
  RoleRepositoryImpl userRepository;

  @Inject
  IdentityProvider keycloakClient;

  @Transactional
  public String register(String name, String description, String clientId) {
    System.out.println(name);
    Role existing = userRepository.findByName(name);
    System.out.println(existing);
    System.out.println("FINDBYNAME");
    if (existing != null) {
      throw new IllegalArgumentException("El Rol ya existe con email: " + name);
    }
    Role rol = Role.createNew(UUID.randomUUID().toString(), name, description, clientId);
    userRepository.save(rol);

    try {
      String externalId = keycloakClient.createRole(name, description, clientId);
      System.out.println(externalId);
      System.out.println("QUE FUEE");
      if (externalId == "") {
        throw new IllegalArgumentException("El rol ya existe en keycloakClient: " + name);
      }
      rol.setSyncStatus(SyncStatus.SYNCED);
      userRepository.update(rol);
    } catch (Exception e) {
      System.out.println("PIPIPI");
      rol.setSyncStatus(SyncStatus.FAILED);
      userRepository.update(rol);
      // No rollback: queremos conservar la persistencia local
    }

    return rol.getName();
  }

  public List<Role> allRolesKeycloak() {
    return keycloakClient.getAllRoles();
  }
}
