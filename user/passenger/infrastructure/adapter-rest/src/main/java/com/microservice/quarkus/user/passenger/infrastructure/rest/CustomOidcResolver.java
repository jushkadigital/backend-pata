package com.microservice.quarkus.user.passenger.infrastructure.rest;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class CustomOidcResolver implements TenantConfigResolver {

  private static final String EVENT_GET_TENANT_CONFIG = "iam.get-client-config";
  private static final String DEFAULT_CLIENT = "dashboard-client";

  private static final String[] PUBLIC_PATHS = {
      "/api/v1/clients",
      "/q/",
      "/webhooks"
  };

  @Inject
  EventBus eventBus;

  @Override
  public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
    String path = context.normalizedPath();

    for (String publicPath : PUBLIC_PATHS) {
      if (path.contains(publicPath)) {
        return Uni.createFrom().nullItem();
      }
    }

    // Request tenant config from IAM module via EventBus
    return eventBus.<JsonObject>request(EVENT_GET_TENANT_CONFIG, DEFAULT_CLIENT)
        .onItem().transform(Message::body)
        .onItem().transform(json -> {
          if (json == null) {
            System.out.println("CustomOidcResolver: No tenant config received from IAM");
            return null;
          }

          System.out.println("CustomOidcResolver: Received config from IAM: " + json.encode());

          OidcTenantConfig config = new OidcTenantConfig();
          config.tenantId = Optional.of(DEFAULT_CLIENT);
          config.setAuthServerUrl(json.getString("authServerUrl"));
          config.clientId = Optional.of(json.getString("clientId"));
          config.credentials.secret = Optional.of(json.getString("clientSecret"));
          config.applicationType = Optional.of(OidcTenantConfig.ApplicationType.SERVICE);

          return config;
        })
        .onFailure().recoverWithItem(error -> {
          System.err.println("CustomOidcResolver: Error getting tenant config via EventBus: " + error.getMessage());
          error.printStackTrace();
          return null;
        });
  }
}
