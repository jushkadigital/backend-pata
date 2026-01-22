package com.microservice.quarkus.catalog.tours.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddServiceCombinationCommand(
    @NotBlank String tourId,
    @NotBlank String name,
    String description,
    @NotBlank String sku,
    @NotEmpty List<String> services,
    Long draftPriceInCents
) {
}
