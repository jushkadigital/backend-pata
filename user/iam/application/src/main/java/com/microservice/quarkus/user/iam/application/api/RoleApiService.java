package com.microservice.quarkus.user.iam.application.api;

import java.util.List;

import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.Role;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserId;

public interface RoleApiService {
  public Role findById(String id);

  public Role findByName(String name);

  public List<Role> getAll();

  public List<Role> getAllById(String clientId);

  public void delete(String id);

  public void update(Role user);

  public void save(Role user);

}
