package com.microservice.quarkus.user.iam.application.service;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.application.dto.CreateUserCommand;
import com.microservice.quarkus.user.iam.domain.*;
import com.microservice.quarkus.user.iam.domain.events.UserRegisteredEvent;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus; // Importante: versión Mutiny
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

  @Inject
  EventBus eventBus;

  @Transactional
  public String register(CreateUserCommand cmd) {
    User existing = userRepository.findByEmail(new EmailAddress(cmd.email()));
    if (existing != null) {
      throw new IllegalArgumentException("El usuario ya existe con email: " + cmd.email());
    }

    User user = User.createNew(cmd.email(), cmd.type());
    userRepository.save(user);

    try {
      // String externalId = keycloakClient.createUser(cmd.email(), cmd.password(),
      // cmd.type());
      user.setExternalId(cmd.externalId());
      user.setSyncStatus(SyncStatus.SYNCED);
      userRepository.update(user);
    } catch (Exception e) {
      user.setSyncStatus(SyncStatus.FAILED);
      userRepository.update(user);
      // No rollback: queremos conservar la persistencia local
    }

    UserRegisteredEvent event = new UserRegisteredEvent(
        user.getExternalId(),
        user.getEmail(),
        user.getType());

    JsonObject oeu = JsonObject.mapFrom(event);
    eventBus.publish("iam.user.registered", oeu.encode());
    return user.getExternalId();
  }

  public List<User> allUsersKeylcloak() {
    return keycloakClient.getAllUsers();
  }

}
