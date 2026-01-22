package com.microservice.quarkus.catalog.tours.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateServiceCommand(
    @NotBlank String tourId,
    @NotBlank String currentServiceName,
    String newServiceName,
    String serviceType,
    Integer quantity,
    Boolean isMandatory,
    Integer durationHours
) {
}
