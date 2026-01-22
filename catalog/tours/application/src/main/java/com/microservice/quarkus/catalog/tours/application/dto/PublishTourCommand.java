package com.microservice.quarkus.catalog.tours.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PublishTourCommand(
    @NotBlank String tourId) {
}
