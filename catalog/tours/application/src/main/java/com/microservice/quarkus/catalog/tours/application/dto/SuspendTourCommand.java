package com.microservice.quarkus.catalog.tours.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SuspendTourCommand(
    @NotBlank String tourId,
    @NotBlank String reason) {
}
