package com.microservice.quarkus.user.application.service;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantConfigResolver {

  @Override
  public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {

    String path = context.request().path();

    if (path.startsWith("/tenant-a")) {

      String keycloakUrl = ConfigProvider.getConfig().getValue("keycloak.url", String.class);

      OidcTenantConfig config = OidcTenantConfig
          .authServerUrl(keycloakUrl + "/realms/tenant-a")
          .tenantId("tenant-a")
          .clientId("multi-tenant-client")
          .credentials("secret")
          .applicationType(ApplicationType.HYBRID)
          .build();
      return Uni.createFrom().item(config);
    } else {
      return Uni.createFrom().nullItem();
    }
  }
}
