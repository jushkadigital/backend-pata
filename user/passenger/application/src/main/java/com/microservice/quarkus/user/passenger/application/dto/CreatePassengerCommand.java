package com.microservice.quarkus.user.passenger.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreatePassengerCommand(
    @NotBlank String externalId,
    @Email String email,
    String type, String subType) {
}
