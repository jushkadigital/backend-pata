package com.microservice.quarkus.user.iam.application.dto;

/**
 * DTO for OIDC tenant configuration shared between IAM and other modules.
 * Used via EventBus for dynamic tenant resolution.
 */
public record TenantConfigDTO(
    String clientId,
    String clientSecret,
    String authServerUrl) {
}
