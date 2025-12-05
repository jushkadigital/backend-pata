package com.microservice.quarkus.user.passenger.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CompletePassengerCommand(
    @NotBlank String firstNames,
    @NotBlank String lastNames,
    @NotBlank String dni,
    @NotBlank String phone) {
}
