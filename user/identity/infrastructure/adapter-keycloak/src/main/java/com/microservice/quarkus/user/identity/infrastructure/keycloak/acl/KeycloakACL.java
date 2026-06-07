package com.microservice.quarkus.user.identity.infrastructure.keycloak.acl;

import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.GroupIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.KeycloakProvider;
import com.microservice.quarkus.user.identity.application.api.RealmIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.RoleIdentityProvider;
import com.microservice.quarkus.user.identity.application.dto.ClientSummary;
import com.microservice.quarkus.user.identity.application.dto.KeycloakRoleDTO;
import com.microservice.quarkus.user.identity.application.dto.KeycloakUserDTO;
import com.microservice.quarkus.user.identity.application.dto.TenantConfigDTO;
import com.microservice.quarkus.user.identity.infrastructure.keycloak.KeycloakService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class KeycloakACL implements KeycloakProvider, RoleIdentityProvider, ClientIdentityProvider, GroupIdentityProvider, RealmIdentityProvider {

  private final KeycloakService keycloakService;
  private final KeycloakUserMapper keycloakUserMapper;
  private final KeycloakRoleMapper keycloakRoleMapper;

  @Inject
  public KeycloakACL(KeycloakService keycloakService,
                      KeycloakUserMapper keycloakUserMapper,
                      KeycloakRoleMapper keycloakRoleMapper) {
    this.keycloakService = keycloakService;
    this.keycloakUserMapper = keycloakUserMapper;
    this.keycloakRoleMapper = keycloakRoleMapper;
  }

  @Override
  public String createUser(String email, String password) {
    return keycloakService.createUser(email, password);
  }

  @Override
  public List<KeycloakUserDTO> getAllUsers() {
    return keycloakService.getUsers().stream().filter(Objects::nonNull).map(keycloakUserMapper::toDTO)
        .collect(Collectors.toList());
  }

  @Override
  public KeycloakUserDTO getUserById(String id) {
    return keycloakUserMapper.toDTO(keycloakService.getUserById(id));
  }

  @Override
  public String createRole(String roleName, String description, String clientId) {
    return keycloakService.findOrCreateRealmRole(roleName, description, clientId);
  }

  @Override
  public String createCompositeRole(String roleName, String description, String clientId, List<String> compositeRoleNames) {
    return keycloakService.createCompositeClientRole(roleName, description, clientId, compositeRoleNames);
  }

  @Override
  public String createClient(String name, List<String> items) {
    return keycloakService.createClient(name, items);
  }

  @Override
  public List<ClientSummary> getClientSummaries() {
    return keycloakService.getClientSummaries();
  }

  @Override
  public TenantConfigDTO getTenantConfig(String clientName) {
    return keycloakService.getTenantConfig(clientName);
  }

  @Override
  public List<KeycloakRoleDTO> getAllRoles() {
    return keycloakService.getRoles().stream().filter(Objects::nonNull).map(keycloakRoleMapper::toDTO)
        .collect(Collectors.toList());
  }

  @Override
  public void assignClientRoleToGroup(String groupId, String clientId, String roleName) {
    keycloakService.assignClientRoleToGroup(groupId, clientId, roleName);
  }

  @Override
  public void assignClientRoleToUser(String userId, String clientId, String roleName) {
    keycloakService.assignClientRoleToUser(userId, clientId, roleName);
  }

  @Override
  public String getToken(String uri) {
    return keycloakService.getToken(uri);
  }

  @Override
  public String getRealm() {
    return keycloakService.getRealm().toString();
  }

  @Override
  public void configurarWebhook(String url) {
    keycloakService.configurarWebhook(url);
  }

  @Override
  public String getClientNameById(String clientId) {
    return keycloakService.getClientNameById(clientId);
  }

  @Override
  public Map<String, String> createGroupHierarchy(List<List<String>> groupHierarchies) {
    return keycloakService.createGroupHierarchy(groupHierarchies);
  }

  @Override
  public String findGroupByPath(String groupPath) {
    return keycloakService.findGroupByPath(groupPath);
  }

  @Override
  public void deleteUser(String externalId) {
    keycloakService.deleteUser(externalId);
  }

  @Override
  public void assignUserToGroup(String userId, String groupId) {
    keycloakService.assignUserToGroup(userId, groupId);
  }

}
