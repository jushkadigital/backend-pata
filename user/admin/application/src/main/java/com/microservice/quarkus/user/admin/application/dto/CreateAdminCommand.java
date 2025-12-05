package com.microservice.quarkus.user.admin.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateAdminCommand(
    @NotBlank String externalId,
    @Email String email,
    String type) {
}
