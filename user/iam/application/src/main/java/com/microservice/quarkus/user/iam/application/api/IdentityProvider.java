package com.microservice.quarkus.user.iam.application.api;

import java.util.List;

import com.microservice.quarkus.user.iam.domain.Role;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserType;

public interface IdentityProvider {
  public String createUser(String email, String password, UserType userType);

  public List<User> getAllUsers();

  public List<Role> getAllRoles();

  public String createRole(String roleName, String Description, String clientId);

  public String createClient(String name);

  public void assingClientRoleToGroup(String groupName, String clientId, String roleName);

  public String getToken();

  public String getRealm();

  public void configurarWebhook(String url);
}
