package com.microservice.quarkus.user.iam.application.dto;

import com.microservice.quarkus.user.iam.domain.UserType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserCommand(
    @Email String email,
    String externalId,
    String password,
    String type,
    String subtype,
    boolean isFromKeycloak) {
  public static CreateUserCommand fromWebhook(@Email String email, String externalId, String type) {
    return new CreateUserCommand(email, externalId, null, type, "STANDARD", true);
  }

  public static CreateUserCommand fromWebhook2(@Email String email, String password, String type, String subtype) {
    return new CreateUserCommand(email, null, password, type, subtype, false);
  }
}
