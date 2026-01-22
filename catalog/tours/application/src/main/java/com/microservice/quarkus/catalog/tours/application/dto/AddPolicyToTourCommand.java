package com.microservice.quarkus.catalog.tours.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AddPolicyToTourCommand(
    @NotBlank String tourId,
    @NotBlank String policyType,
    @NotBlank String description,
    String value) {
}
