package com.microservice.quarkus.user.iam.infrastructure.keycloak.acl;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.application.dto.TenantConfigDTO;
import com.microservice.quarkus.user.iam.domain.Role;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserType;
import com.microservice.quarkus.user.iam.infrastructure.keycloak.KeycloakService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class KeycloakACL implements IdentityProvider {

  @Inject
  KeycloakService keycloakService;

  @Inject
  KeycloakUserMapper keycloakUserMapper;

  @Inject
  KeycloakRoleMapper keycloakRoleMapper;

  @Override
  public String createUser(String email, String password, UserType typeUser) {
    String kcUserId = keycloakService.createUser(email, password, typeUser);
    return kcUserId;
  }

  @Override
  public List<User> getAllUsers() {
    return keycloakService.getUsers().stream().filter(Objects::nonNull).map(keycloakUserMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public String createRole(String roleName, String description, String clientId) {
    return keycloakService.findOrCreateRealmRole(roleName, description, clientId);
  }

  @Override
  public String createClient(String name) {
    return keycloakService.createClient(name);
  }

  @Override
  public Map<String, ?> getClientsCreatedByMe() {
    return keycloakService.getClientsCreatedByMe();
  }

  @Override
  public TenantConfigDTO getTenantConfig(String clientName) {
    return keycloakService.getTenantConfig(clientName);
  }

  @Override
  public List<Role> getAllRoles() {
    return keycloakService.getRoles().stream().filter(Objects::nonNull).map(keycloakRoleMapper::toDomain)
        .collect(Collectors.toList());

  }

  @Override
  public void assingClientRoleToGroup(String groupName, String clientId, String roleName) {
    keycloakService.assignClientRoleToGroup(groupName, clientId, roleName);
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
  public User getUserById(String id) {
    return keycloakUserMapper.toDomain(keycloakService.getUserById(id));
  }

}
