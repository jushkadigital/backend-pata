package com.microservice.quarkus.user.admin.application.service;

import java.util.List;

import com.microservice.quarkus.admin.domain.entities.Admin;
import com.microservice.quarkus.admin.domain.entities.AdminId;
import com.microservice.quarkus.admin.domain.entities.EmailAddress;
import com.microservice.quarkus.admin.domain.ports.out.AdminRepository;
import com.microservice.quarkus.user.admin.application.api.AdminApiService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AdminRepositoryImpl implements AdminApiService {

  AdminRepository userRepository;

  public AdminRepositoryImpl(AdminRepository userRepository) {
    this.userRepository = userRepository;
  }

  public Admin findById(AdminId id) {
    return userRepository.findById(id);
  }

  public Admin findByEmail(EmailAddress email) {
    return userRepository.findByEmail(email);
  }

  @Override
  public void delete(String id) {

    userRepository.delete(id);
  }

  @Override
  public List<Admin> getAll() {
    return userRepository.getAll();
  }

  @Override
  public void save(Admin loan) {

    userRepository.save(loan);

  }

  @Override
  public void update(Admin loan) {

    userRepository.update(loan);
  }
}
