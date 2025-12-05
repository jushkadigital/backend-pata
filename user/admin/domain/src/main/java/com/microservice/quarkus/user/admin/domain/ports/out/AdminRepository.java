package com.microservice.quarkus.user.admin.domain.ports.out;

import java.util.*;

import com.microservice.quarkus.user.admin.domain.entities.Admin;
import com.microservice.quarkus.user.admin.domain.entities.AdminId;
import com.microservice.quarkus.user.admin.domain.entities.EmailAddress;

public interface AdminRepository {
  public Admin findById(AdminId id);

  public Admin findByEmail(EmailAddress email);

  public List<Admin> getAll();

  void save(Admin user);

  void update(Admin user);

  void delete(String id);
}
