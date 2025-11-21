
package com.microservice.quarkus.user.iam.application.api;

import java.util.List;

import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserId;

public interface UserApiService {
  public User findById(UserId id);

  public User findByEmail(EmailAddress email);

  public List<User> getAll();

  public void delete(String id);

  public void update(User user);

  public void save(User user);

}
