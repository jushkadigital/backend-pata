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

  public record RoleDefinition(String name, String description) {
  }

  public record GroupRoleAssignment(String groupPath, List<String> roles) {
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

  void onStart(@Observes @Priority(10) StartupEvent ev) {
    log.info(">>(10) Iniciando Identity Bootstrap...");
    log.info(">>(10) Profile: {}", profile);
    log.info(">>(10) Keycloak server-url: {}", keycloakServerUrl);

    try {
      if ("dev".equals(profile) || "test".equals(profile)) {
        log.info(">>(10) Dev/Test mode detected, waiting for Keycloak connection...");
        if (!waitForKeycloak()) {

          log.error("Identity Bootstrap: Failed to connect to Keycloak after retries");
          return;
        }
      } else {

        realmIdentityProvider.getRealm();
      }

      initializeKeycloak();
    } catch (Exception e) {
      log.error("Identity Bootstrap: Error during initialization: {}", e.getMessage(), e);
    }
  }

  private boolean waitForKeycloak() {
    for (int i = 1; i <= MAX_RETRIES; i++) {
      try {
        log.info("Identity Bootstrap: Attempting to connect to Keycloak at {} (attempt {}/{})", keycloakServerUrl, i, MAX_RETRIES);
        realmIdentityProvider.getRealm();
        log.info("Identity Bootstrap: Connected to Keycloak successfully");
        return true;
      } catch (Exception e) {
        if (i == 1) {
          log.error("Identity Bootstrap: Keycloak connection failed - URL: {} | Error: {}", keycloakServerUrl, e.getMessage(), e);
        } else {
          log.info("Identity Bootstrap: Keycloak not ready yet - {}", e.getClass().getSimpleName());
        }
        if (i < MAX_RETRIES) {
          try {
            Thread.sleep(RETRY_DELAY_MS);
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

    List<List<String>> hierarchy = List.of(
        List.of("passenger", "premium"),
        List.of("passenger", "standard"),
        List.of("passenger", "basic"),
        List.of("admin", "super-admin"),
        List.of("admin", "admin"),
        List.of("admin", "editor"));

    Map<String, String> groupIds = groupIdentityProvider.createGroupHierarchy(hierarchy);

    log.info("Identity Bootstrap: Groups created: {}", groupIds);

    if (clientId != null && !clientId.isEmpty() && clientId2 != null && !clientId2.isEmpty()) {
      List<RoleDefinition> rolesAdmin = List.of(
          new RoleDefinition("ADMIN_VIEW", "Permite visualizar el panel de administración"),
          new RoleDefinition("ADMIN_COMMAND", "Permite ejecutar comandos administrativos"),
          new RoleDefinition("CATALOG_CREATE", "Permite crear elementos en el catálogo"),
          new RoleDefinition("CATALOG_ERASE", "Permite eliminar elementos del catálogo"));

      List<RoleDefinition> rolesPassenger = List.of(
          new RoleDefinition("CATALOG_VIEW", "Permite visualizar el catálogo"),
          new RoleDefinition("FORM_COMPLETE", "Permite completar formularios"));

      log.info("Identity Bootstrap: Creating admin roles...");
      for (RoleDefinition role : rolesAdmin) {
        roleService.register(role.name(), role.description(), clientId);
      }

      log.info("Identity Bootstrap: Creating passenger roles...");
      for (RoleDefinition role : rolesPassenger) {
        roleService.register(role.name(), role.description(), clientId2);
      }

      List<GroupRoleAssignment> adminPermissions = List.of(
          new GroupRoleAssignment("admin/super-admin",
              List.of("ADMIN_VIEW", "ADMIN_COMMAND", "CATALOG_CREATE", "CATALOG_ERASE")),
          new GroupRoleAssignment("admin/admin",
              List.of("ADMIN_VIEW", "ADMIN_COMMAND", "CATALOG_CREATE")),
          new GroupRoleAssignment("admin/editor",
              List.of("ADMIN_VIEW", "CATALOG_CREATE")));

      List<GroupRoleAssignment> passengerPermissions = List.of(
          new GroupRoleAssignment("passenger/premium",
              List.of("CATALOG_VIEW", "FORM_COMPLETE")),
          new GroupRoleAssignment("passenger/standard",
              List.of("CATALOG_VIEW")),
          new GroupRoleAssignment("passenger/basic",
              List.of("CATALOG_VIEW")));

      log.info("Identity Bootstrap: Assigning admin permissions...");
      for (GroupRoleAssignment assignment : adminPermissions) {
        String groupId = groupIds.get(assignment.groupPath());
        if (groupId != null) {
          for (String roleName : assignment.roles()) {
            roleIdentityProvider.assignClientRoleToGroup(groupId, clientId, roleName);
            log.info("  ✓ Assigned {} to {}", roleName, assignment.groupPath());
          }
        } else {
          log.error("  ✗ Group not found: {}", assignment.groupPath());
        }
      }

      log.info("Identity Bootstrap: Assigning passenger permissions...");
      for (GroupRoleAssignment assignment : passengerPermissions) {
        String groupId = groupIds.get(assignment.groupPath());
        if (groupId != null) {
          for (String roleName : assignment.roles()) {
            roleIdentityProvider.assignClientRoleToGroup(groupId, clientId2, roleName);
            log.info("  ✓ Assigned {} to {}", roleName, assignment.groupPath());
          }
        } else {
          log.error("  ✗ Group not found: {}", assignment.groupPath());
        }
      }
    }

    log.info("Identity Bootstrap: Initialization complete");
  }
}
