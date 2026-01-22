package com.microservice.quarkus.catalog.tours.application.dto;

import jakarta.validation.constraints.NotBlank;

public record RemoveServiceCommand(
    @NotBlank String tourId,
    @NotBlank String serviceName
) {
}
