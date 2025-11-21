package com.microservice.quarkus.user.iam.application.service;

import java.util.List;

import com.microservice.quarkus.user.iam.application.api.RoleApiService;
import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.RoleRepository;
import com.microservice.quarkus.user.iam.domain.Role;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RoleRepositoryImpl implements RoleApiService {

  RoleRepository roleRepository;

  public RoleRepositoryImpl(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  public Role findById(String id) {
    return roleRepository.findById(id);
  }

  public Role findByName(String name) {
    return roleRepository.findByName(name);
  }

  @Override
  public void delete(String id) {

    roleRepository.delete(id);
  }

  @Override
  public List<Role> getAll() {
    return roleRepository.getAll();
  }

  @Override
  public List<Role> getAllById(String clientId) {
    return roleRepository.getAllById(clientId);
  }

  @Override
  public void save(Role loan) {

    roleRepository.save(loan);

  }

  @Override
  public void update(Role loan) {

    roleRepository.update(loan);
  }
}
