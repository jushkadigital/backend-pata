
package com.microservice.quarkus.user.iam.domain;

import java.util.*;

public interface RoleRepository {
  public Role findById(String id);

  public Role findByName(String name);

  public List<Role> getAll();

  public List<Role> getAllById(String id);

  public void save(Role role);

  public void update(Role role);

  public void delete(String id);
}
