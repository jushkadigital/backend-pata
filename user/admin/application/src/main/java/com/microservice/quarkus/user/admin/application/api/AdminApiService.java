package com.microservice.quarkus.user.admin.application.api;

import java.util.List;

import com.microservice.quarkus.user.admin.domain.entities.Admin;
import com.microservice.quarkus.user.admin.domain.entities.AdminId;
import com.microservice.quarkus.user.admin.domain.entities.EmailAddress;

public interface AdminApiService {
  public Admin findById(AdminId id);

  public Admin findByEmail(EmailAddress email);

  public List<Admin> getAll();

  public void delete(String id);

  public void update(Admin user);

  public void save(Admin user);
}
