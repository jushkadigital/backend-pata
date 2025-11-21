package com.microservice.quarkus.user.admin.application.service;

import com.microservice.quarkus.admin.domain.entities.Admin;
import com.microservice.quarkus.admin.domain.entities.AdminId;
import com.microservice.quarkus.admin.domain.entities.AdminType;
import com.microservice.quarkus.admin.domain.entities.EmailAddress;
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
  public String register(String id, String email, String adminType) {
    Admin existing = userRepository.findById(AdminId.of(id));
    if (existing != null) {
      throw new IllegalArgumentException("El Admin user ya existe con email: " + email);
    }

    Admin user = Admin.createNew(AdminId.random(), new EmailAddress(email), AdminType.valueOf(adminType.toUpperCase()));
    userRepository.save(user);

    return user.getId().value();
  }

}
