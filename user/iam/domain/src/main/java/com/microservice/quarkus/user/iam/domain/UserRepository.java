
package com.microservice.quarkus.user.iam.domain;

import java.util.*;

public interface UserRepository {
  public User findById(UserId id);

  public User findByEmail(EmailAddress email);

  public List<User> getAll();

  void save(User user);

  void update(User user);

  void delete(String id);
}
