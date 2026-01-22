package com.microservice.quarkus.catalog.tours.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTourCommand(
    @NotBlank String tourId,
    String name,
    String description,
    Integer durationHours
) {
}
