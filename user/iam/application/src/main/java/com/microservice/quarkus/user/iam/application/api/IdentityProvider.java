package com.microservice.quarkus.user.iam.application.api;

import java.util.List;
import java.util.Map;

import com.microservice.quarkus.user.iam.application.dto.TenantConfigDTO;
import com.microservice.quarkus.user.iam.domain.Role;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserType;

public interface IdentityProvider {
  public String createUser(String email, String password);

  public List<User> getAllUsers();

  public List<Role> getAllRoles();

  public String createRole(String roleName, String Description, String clientId);

  public String createClient(String name,List<String> items);

  public Map<String, ?> getClientsCreatedByMe();

  public TenantConfigDTO getTenantConfig(String clientName);

  public void assingClientRoleToGroup(String groupId, String clientId, String roleName);

  public String getToken(String uri);

  public String getRealm();

  public void configurarWebhook(String url);

  public String getClientNameById(String clientId);

  public User getUserById(String id);

  public Map<String, String> createGroupHierarchy(List<List<String>> groupHierarchies);

  public String findGroupByPath(String groupPath);

  public void assignUserToGroup(String userId, String groupId);
}
