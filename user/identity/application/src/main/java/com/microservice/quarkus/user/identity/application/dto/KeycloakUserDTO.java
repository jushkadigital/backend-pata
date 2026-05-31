package com.microservice.quarkus.user.identity.application.dto;

public record KeycloakUserDTO(
    String id,
    String email,
    String externalId,
    String type
) {}
