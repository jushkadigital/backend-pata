package com.microservice.quarkus.user.iam.application.service;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.domain.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserService {

  @Inject
  UserRepositoryImpl userRepository;

  @Inject
  IdentityProvider keycloakClient;

  @Transactional
  public String register(String email, String password, UserType type) {
    User existing = userRepository.findByEmail(new EmailAddress(email));
    if (existing != null) {
      throw new IllegalArgumentException("El usuario ya existe con email: " + email);
    }

    User user = User.createNew(new UserId(UUID.randomUUID().toString()), new EmailAddress(email), type);
    userRepository.save(user);

    try {
      String externalId = keycloakClient.createUser(email, password, type);
      if (externalId == "") {
        throw new IllegalArgumentException("El usuario ya existe en keycloakClient: " + email);
      }
      user.setExternalId(externalId);
      user.setSyncStatus(SyncStatus.SYNCED);
      userRepository.update(user);
    } catch (Exception e) {
      user.setSyncStatus(SyncStatus.FAILED);
      userRepository.update(user);
      // No rollback: queremos conservar la persistencia local
    }

    return user.getExternalId();
  }

  public List<User> allUsersKeylcloak() {
    return keycloakClient.getAllUsers();
  }

}
