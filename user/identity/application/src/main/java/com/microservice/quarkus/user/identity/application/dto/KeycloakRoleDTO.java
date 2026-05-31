package com.microservice.quarkus.user.identity.application.dto;

public record KeycloakRoleDTO(
    String id,
    String name,
    String description,
    String clientId
) {}
