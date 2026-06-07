package com.microservice.quarkus.user.identity.application.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserCommand(
    @Email String email,
    String externalId,
    String password,
    String userType,      // NEW: PASSENGER or ADMIN
    Set<String> roles,    // NOW: only Keycloak composite roles
    boolean isFromKeycloak) {
  public static CreateUserCommand fromWebhook(@Email String email, String externalId, String userType, Set<String> roles) {
    return new CreateUserCommand(email, externalId, null, userType, roles, true);
  }

  public static CreateUserCommand fromWebhook2(@Email String email, String password, String userType, Set<String> roles) {
    return new CreateUserCommand(email, null, password, userType, roles, false);
  }
}
