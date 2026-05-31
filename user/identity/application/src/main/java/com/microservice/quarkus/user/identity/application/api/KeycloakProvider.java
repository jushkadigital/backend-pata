package com.microservice.quarkus.user.identity.application.api;

import java.util.List;

import com.microservice.quarkus.user.identity.application.dto.KeycloakUserDTO;

public interface KeycloakProvider {
  String createUser(String email, String password);

  List<KeycloakUserDTO> getAllUsers();

  KeycloakUserDTO getUserById(String id);

  void deleteUser(String externalId);
}
