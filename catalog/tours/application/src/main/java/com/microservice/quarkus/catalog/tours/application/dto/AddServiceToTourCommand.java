package com.microservice.quarkus.catalog.tours.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.Map;

public record AddServiceToTourCommand(
    @NotBlank String tourId,
    @NotBlank String serviceType,
    @NotBlank String serviceName,
    @Positive int quantity,
    boolean isMandatory,
    Integer durationHours,
    Map<String, Object> configuration
) {
    public AddServiceToTourCommand {
        if (quantity < 1) {
            quantity = 1;
        }
    }
}
