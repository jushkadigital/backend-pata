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
  IdentityProvider keycloakClient;

  @Inject
  EventBus eventBus;

  private UserRepository userRepository;

  @Inject
  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public String register(CreateUserCommand cmd) {
    User existing = userRepository.findByEmail(new EmailAddress(cmd.email()));
    if (existing != null) {
      if (cmd.type().equals(existing.getType().toString())) {
        throw new IllegalArgumentException("El usuario ya existe con email: " + cmd.email());
      } else {

      }
    }

    User user = User.createNew(cmd.email(), cmd.type());
    userRepository.save(user);
    String externalId;
    try {
      if (!cmd.isFromKeycloak()) {
        externalId = keycloakClient.createUser(cmd.email(), cmd.password());
      } else {
        externalId = cmd.externalId();
      }
      user.setExternalId(externalId);
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
        cmd.type(), cmd.subtype());

    JsonObject oeu = JsonObject.mapFrom(event);
    eventBus.publish("iam.user.registered", oeu.encode());
    return user.getExternalId();
  }

  public List<User> allUsersKeylcloak() {
    return keycloakClient.getAllUsers();
  }

}
