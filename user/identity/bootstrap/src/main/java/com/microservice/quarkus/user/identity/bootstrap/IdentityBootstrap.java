package com.microservice.quarkus.user.identity.bootstrap;

import java.util.List;
import java.util.Map;

import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.GroupIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.KeycloakProvider;
import com.microservice.quarkus.user.identity.application.api.RealmIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.RoleIdentityProvider;
import com.microservice.quarkus.user.identity.application.dto.CreateUserCommand;
import com.microservice.quarkus.user.identity.application.service.RoleService;
import com.microservice.quarkus.user.identity.application.service.UserService;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IdentityBootstrap {

  private static final Logger log = LoggerFactory.getLogger(IdentityBootstrap.class);

  private static final int MAX_RETRIES = 200;
  private static final int RETRY_DELAY_MS = 3000;
  private static final int PROD_MAX_RETRIES = 10;
  private static final int PROD_RETRY_DELAY_MS = 5000;

  public record RoleDefinition(String name, String description) {
  }

  public record CompositeRoleDefinition(String name, String description, List<String> includes) {
  }

  @Inject
  RoleService roleService;

  @Inject
  KeycloakProvider keycloakProvider;

  @Inject
  RoleIdentityProvider roleIdentityProvider;

  @Inject
  ClientIdentityProvider clientIdentityProvider;

  @Inject
  GroupIdentityProvider groupIdentityProvider;

  @Inject
  RealmIdentityProvider realmIdentityProvider;

  @Inject
  UserService userService;

  @ConfigProperty(name = "quarkus.profile", defaultValue = "prod")
  String profile;

  @ConfigProperty(name = "quarkus.keycloak.admin-client.server-url", defaultValue = "NOT_SET")
  String keycloakServerUrl;

  @ActivateRequestContext
  void onStart(@Observes @Priority(10) StartupEvent ev) {
    log.info(">>(10) Iniciando Identity Bootstrap...");
    log.info(">>(10) Profile: {}", profile);
    log.info(">>(10) Keycloak server-url: {}", keycloakServerUrl);

    try {
      if ("dev".equals(profile) || "test".equals(profile)) {
        log.info(">>(10) Dev/Test mode detected, waiting for Keycloak connection...");
        if (!waitForKeycloak(MAX_RETRIES, RETRY_DELAY_MS)) {

          log.error("Identity Bootstrap: Failed to connect to Keycloak after retries");
          return;
        }
      } else {
        log.info(">>(10) Prod mode detected, verifying Keycloak connection...");
        if (!waitForKeycloak(PROD_MAX_RETRIES, PROD_RETRY_DELAY_MS)) {
          log.error("Identity Bootstrap: Failed to connect to Keycloak after retries");
          return;
        }
      }

      initializeKeycloak();
    } catch (Exception e) {
      log.error("Identity Bootstrap: Error during initialization: {}", e.getMessage(), e);
    }
  }

  private boolean waitForKeycloak(int maxRetries, int retryDelayMs) {
    for (int i = 1; i <= maxRetries; i++) {
      try {
        log.info("Identity Bootstrap: Attempting to connect to Keycloak at {} (attempt {}/{})", keycloakServerUrl, i, maxRetries);
        realmIdentityProvider.getRealm();
        log.info("Identity Bootstrap: Connected to Keycloak successfully");
        return true;
      } catch (Exception e) {
        if (i == 1) {
          log.error("Identity Bootstrap: Keycloak connection failed - URL: {} | Error: {}", keycloakServerUrl, e.getMessage(), e);
        } else {
          log.info("Identity Bootstrap: Keycloak not ready yet - {}", e.getClass().getSimpleName());
        }
        if (i < maxRetries) {
          try {
            Thread.sleep(retryDelayMs);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
          }
        }
      }
    }
    return false;
  }

  private void initializeKeycloak() {
    log.info("Identity Bootstrap: Realm created/verified");

    String webhookUrl = "dev".equals(profile) || "test".equals(profile)
        ? "http://172.17.0.1:8081/webhooks/keycloak"
        : "http://quarkus-app-prod:8081/webhooks/keycloak";

    realmIdentityProvider.configurarWebhook(webhookUrl);
    realmIdentityProvider.getToken(webhookUrl);

    List<String> dashboardRedirects = List.of("http://localhost:3000/*", "http://localhost:9000/*", "https://cms.patarutera.pe/*", "https://commerce.patarutera.pe/*", "https://www.cms.patarutera.pe/*", "https://www.commerce.patarutera.pe/*");
    String clientId = clientIdentityProvider.createClient("dashboard-client", dashboardRedirects);
    log.info("Identity Bootstrap: Client created with ID: {}", clientId);

    List<String> frontRedirects = List.of("http://localhost:4000/*", "http://localhost:9000/*", "https://www.patarutera.pe/", "https://patarutera.pe/*", "https://patarutera-o3it-git-login-josues-projects-cd2f3b7d.vercel.app/*", "https://www.patarutera-o3it-git-login-josues-projects-cd2f3b7d.vercel.app/*");
    String clientId2 = clientIdentityProvider.createClient("frontend-client", frontRedirects);
    log.info("Identity Bootstrap: Client created with ID: {}", clientId2);

    List<List<String>> flatGroups = List.of(
        List.of("admins"),
        List.of("passengers"));

    Map<String, String> groupIds = groupIdentityProvider.createGroupHierarchy(flatGroups);

    log.info("Identity Bootstrap: Groups created: {}", groupIds);

    if (clientId != null && !clientId.isEmpty() && clientId2 != null && !clientId2.isEmpty()) {
      List<RoleDefinition> adminPermissions = List.of(
          new RoleDefinition("ADMIN_VIEW", "Permite visualizar el panel de administración"),
          new RoleDefinition("ADMIN_COMMAND", "Permite ejecutar comandos administrativos"),
          new RoleDefinition("CATALOG_CREATE", "Permite crear elementos en el catálogo"),
          new RoleDefinition("CATALOG_ERASE", "Permite eliminar elementos del catálogo"),
          new RoleDefinition("CMS_ACCESS", "Permite acceder al CMS"));

      List<RoleDefinition> passengerPermissions = List.of(
          new RoleDefinition("CATALOG_VIEW", "Permite visualizar el catálogo"),
          new RoleDefinition("FORM_COMPLETE", "Permite completar formularios"));

      log.info("Identity Bootstrap: Creating admin permission roles...");
      for (RoleDefinition role : adminPermissions) {
        roleService.register(role.name(), role.description(), clientId);
      }

      log.info("Identity Bootstrap: Creating passenger permission roles...");
      for (RoleDefinition role : passengerPermissions) {
        roleService.register(role.name(), role.description(), clientId2);
      }

      List<CompositeRoleDefinition> adminRoles = List.of(
          new CompositeRoleDefinition("super-admin", "Administrador con acceso total",
              List.of("ADMIN_VIEW", "ADMIN_COMMAND", "CATALOG_CREATE", "CATALOG_ERASE", "CMS_ACCESS")),
          new CompositeRoleDefinition("admin", "Administrador con acceso de gestión",
              List.of("ADMIN_VIEW", "ADMIN_COMMAND", "CATALOG_CREATE", "CMS_ACCESS")),
          new CompositeRoleDefinition("editor", "Editor con acceso de creación",
              List.of("ADMIN_VIEW", "CATALOG_CREATE", "CMS_ACCESS")));

      List<CompositeRoleDefinition> passengerRoles = List.of(
          new CompositeRoleDefinition("premium", "Pasajero premium con acceso completo",
              List.of("CATALOG_VIEW", "FORM_COMPLETE")),
          new CompositeRoleDefinition("standard", "Pasajero estándar con acceso básico",
              List.of("CATALOG_VIEW")),
          new CompositeRoleDefinition("basic", "Pasajero básico con acceso mínimo",
              List.of("CATALOG_VIEW")));

      log.info("Identity Bootstrap: Creating admin composite roles...");
      for (CompositeRoleDefinition role : adminRoles) {
        roleService.registerComposite(role.name(), role.description(), clientId, role.includes());
      }

      log.info("Identity Bootstrap: Creating passenger composite roles...");
      for (CompositeRoleDefinition role : passengerRoles) {
        roleService.registerComposite(role.name(), role.description(), clientId2, role.includes());
      }

      String adminsGroupId = groupIds.get("admins");
      String passengersGroupId = groupIds.get("passengers");

      if (adminsGroupId != null) {
        log.info("Identity Bootstrap: Assigning editor role to admins group (base role)...");
        roleIdentityProvider.assignClientRoleToGroup(adminsGroupId, clientId, "editor");
      } else {
        log.error("Identity Bootstrap: Group 'admins' not found");
      }

      if (passengersGroupId != null) {
        log.info("Identity Bootstrap: Assigning basic role to passengers group (base role)...");
        roleIdentityProvider.assignClientRoleToGroup(passengersGroupId, clientId2, "basic");
      } else {
        log.error("Identity Bootstrap: Group 'passengers' not found");
      }
    }

    log.info("Identity Bootstrap: Initialization complete");
  }
}
