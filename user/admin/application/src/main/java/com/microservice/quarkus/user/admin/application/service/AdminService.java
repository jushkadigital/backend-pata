package com.microservice.quarkus.user.admin.application.service;

import com.microservice.quarkus.user.admin.domain.entities.Admin;
import com.microservice.quarkus.user.admin.domain.entities.AdminId;
import com.microservice.quarkus.user.admin.domain.entities.AdminType;
import com.microservice.quarkus.user.admin.domain.entities.EmailAddress;
import com.microservice.quarkus.user.admin.application.dto.CreateAdminCommand;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AdminService {

  @Inject
  AdminRepositoryImpl userRepository;

  @Transactional
  public String register(CreateAdminCommand cmd) {
    Admin existing = userRepository.findByEmail(new EmailAddress(cmd.email()));
    if (existing != null) {
      throw new IllegalArgumentException("El Admin user ya existe con email: " + cmd.email());
    }

    Admin user = Admin.createNew(cmd.externalId(), cmd.email(), cmd.type());
    userRepository.save(user);

    return user.getId().value();
  }

}
