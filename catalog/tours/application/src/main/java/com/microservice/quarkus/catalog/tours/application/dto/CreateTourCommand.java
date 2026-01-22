package com.microservice.quarkus.catalog.tours.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateTourCommand(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    @Min(1) @Max(24) int durationHours,
    List<ServiceCommand> services) {

    public CreateTourCommand(
            String code,
            String name,
            String description,
            int durationHours) {
        this(code, name, description, durationHours, List.of());
    }
}
