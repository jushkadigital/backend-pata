package com.microservice.quarkus.user.identity.application.dto;

/**
 * DTO for OIDC tenant configuration shared between Identity and other modules.
 * Used via EventBus for dynamic tenant resolution.
 *
 * <p>The {@code clientSecret} field is transmitted exclusively through the
 * in-process Vert.x EventBus (not over the network). In production environments
 * this secret should ideally be retrieved from Vault instead of being passed
 * through messages.</p>
 */
public record TenantConfigDTO(
    String clientId,
    String clientSecret,
    String authServerUrl) {
}
