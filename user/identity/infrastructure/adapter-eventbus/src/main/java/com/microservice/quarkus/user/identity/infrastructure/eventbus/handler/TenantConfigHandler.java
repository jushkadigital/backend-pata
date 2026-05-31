package com.microservice.quarkus.user.identity.infrastructure.eventbus.handler;

import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.application.dto.TenantConfigDTO;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TenantConfigHandler {

  private static final Logger log = LoggerFactory.getLogger(TenantConfigHandler.class);

  private final ClientIdentityProvider clientIdentityProvider;

  private static final String DEFAULT_CLIENT = "dashboard-client";

  @Inject
  public TenantConfigHandler(ClientIdentityProvider clientIdentityProvider) {
    this.clientIdentityProvider = clientIdentityProvider;
  }

  @ConsumeEvent(value = "identity.get-client-config", blocking = true)
  @ActivateRequestContext
  public JsonObject getTenantConfig(String clientName) {
    log.info("EventBus Identity: Requesting tenant config for: {}", clientName);

    String targetClient = (clientName == null || clientName.isBlank()) ? DEFAULT_CLIENT : clientName;

    TenantConfigDTO config = clientIdentityProvider.getTenantConfig(targetClient);

    if (config == null) {
      log.info("EventBus Identity: No config found for client: {}", targetClient);
      return null;
    }

    log.info("EventBus Identity: Returning config for client: {}", targetClient);

    return new JsonObject()
        .put("clientId", config.clientId())
        .put("clientSecret", config.clientSecret())
        .put("authServerUrl", config.authServerUrl());
  }
}
