package com.microservice.quarkus.user.iam.infrastructure.eventbus.consumer;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.application.dto.TenantConfigDTO;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserRegisteredListener {

  @Inject
  IdentityProvider keycloakService;

  private static final String DEFAULT_CLIENT = "dashboard-client";

  @ConsumeEvent(value = "iam.get-client-config", blocking = true)
  @ActivateRequestContext
  public JsonObject getTenantConfig(String clientName) {
    System.out.println("EventBus IAM: Requesting tenant config for: " + clientName);

    String targetClient = (clientName == null || clientName.isBlank()) ? DEFAULT_CLIENT : clientName;

    TenantConfigDTO config = keycloakService.getTenantConfig(targetClient);

    if (config == null) {
      System.out.println("EventBus IAM: No config found for client: " + targetClient);
      return null;
    }

    System.out.println("EventBus IAM: Returning config for client: " + targetClient);

    // Return as JsonObject to avoid codec issues between modules
    return new JsonObject()
        .put("clientId", config.clientId())
        .put("clientSecret", config.clientSecret())
        .put("authServerUrl", config.authServerUrl());
  }
}
