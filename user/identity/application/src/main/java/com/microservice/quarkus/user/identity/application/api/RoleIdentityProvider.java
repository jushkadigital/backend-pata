package com.microservice.quarkus.user.identity.application.api;

import java.util.List;

import com.microservice.quarkus.user.identity.application.dto.KeycloakRoleDTO;

public interface RoleIdentityProvider {
  String createRole(String roleName, String description, String clientId);

  List<KeycloakRoleDTO> getAllRoles();

  void assignClientRoleToGroup(String groupId, String clientId, String roleName);
}
