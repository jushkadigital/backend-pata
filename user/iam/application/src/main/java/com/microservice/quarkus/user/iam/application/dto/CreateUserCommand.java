package com.microservice.quarkus.user.iam.application.dto;

import com.microservice.quarkus.user.iam.domain.UserType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserCommand(
    @Email String email,
    @NotBlank String externalId,
    @NotBlank String type) {
}
