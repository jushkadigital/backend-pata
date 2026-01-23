package com.microservice.quarkus.user.iam.bootstrap;

import java.util.List;
import java.util.Map;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.application.dto.CreateUserCommand;
import com.microservice.quarkus.user.iam.application.service.RoleService;
import com.microservice.quarkus.user.iam.application.service.UserService;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class IamBootstrap {

  private static final int MAX_RETRIES = 80;
  private static final int RETRY_DELAY_MS = 3000;

  public record RoleDefinition(String name, String description) {
  }

  public record GroupRoleAssignment(String groupPath, List<String> roles) {
  }

  @Inject
  RoleService roleService;

  @Inject
  IdentityProvider clientService;

  @Inject
  UserService userService;

  void onStart(@Observes @Priority(10) StartupEvent ev) {
    System.out.println(">> (10) Iniciando IAM Bootstrap...");

    if (!waitForKeycloak()) {
      System.err.println("IAM Bootstrap: Could not connect to Keycloak after " + MAX_RETRIES + " attempts");
      return;
    }

    try {
      initializeKeycloak();
    } catch (Exception e) {
      System.err.println("IAM Bootstrap: Error during initialization: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private boolean waitForKeycloak() {
    for (int i = 1; i <= MAX_RETRIES; i++) {
      try {
        System.out.println("IAM Bootstrap: Attempting to connect to Keycloak (attempt " + i + "/" + MAX_RETRIES + ")");
        clientService.getRealm();
        System.out.println("IAM Bootstrap: Connected to Keycloak successfully");
        return true;
      } catch (Exception e) {
        System.out.println("IAM Bootstrap: Keycloak not ready yet - " + e.getClass().getSimpleName());
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
    System.out.println("IAM Bootstrap: Realm created/verified");

    //clientService.configurarWebhook("http://172.17.0.1:8081/webhooks/keycloak");
    clientService.configurarWebhook("http://quarkus-app-prod:8081/webhooks/keycloak");
    //clientService.getToken("http://172.17.0.1:8081/webhooks/keycloak");
    clientService.getToken("http://quarkus-app-prod:8081/webhooks/keycloak");

    List<String> dashboardRedirects = List.of("http://localhost:3000/*", "http://localhost:9000/*");
    String clientId = clientService.createClient("dashboard-client", dashboardRedirects);
    System.out.println("IAM Bootstrap: Client created with ID: " + clientId);

    List<String> frontRedirects = List.of("http://localhost:4000/*","http://localhost:9000/*");
    String clientId2 = clientService.createClient("frontend-client", frontRedirects);
    System.out.println("IAM Bootstrap: Client created with ID: " + clientId2);

    List<List<String>> hierarchy = List.of(
        // Grupo Passenger con sus subgrupos
        List.of("passenger", "premium"),
        List.of("passenger", "standard"),
        List.of("passenger", "basic"),

        // Grupo Admin con sus subgrupos
        List.of("admin", "super-admin"),
        List.of("admin", "admin"),
        List.of("admin", "editor"));

    Map<String, String> groupIds = clientService.createGroupHierarchy(hierarchy);

    System.out.println("IAM Bootstrap: Groups created: " + groupIds);

    if (clientId != null && !clientId.isEmpty() && clientId2 != null && !clientId2.isEmpty()) {
      // Definir roles y crearlos en Keycloak
      List<RoleDefinition> rolesAdmin = List.of(
          new RoleDefinition("ADMIN_VIEW", "Permite visualizar el panel de administración"),
          new RoleDefinition("ADMIN_COMMAND", "Permite ejecutar comandos administrativos"),
          new RoleDefinition("CATALOG_CREATE", "Permite crear elementos en el catálogo"),
          new RoleDefinition("CATALOG_ERASE", "Permite eliminar elementos del catálogo"));

      List<RoleDefinition> rolesPassenger = List.of(
          new RoleDefinition("CATALOG_VIEW", "Permite visualizar el catálogo"),
          new RoleDefinition("FORM_COMPLETE", "Permite completar formularios"));

      // Crear roles en Keycloak
      System.out.println("IAM Bootstrap: Creating admin roles...");
      for (RoleDefinition role : rolesAdmin) {
        roleService.register(role.name(), role.description(), clientId);
      }

      System.out.println("IAM Bootstrap: Creating passenger roles...");
      for (RoleDefinition role : rolesPassenger) {
        roleService.register(role.name(), role.description(), clientId2);
      }

      // Definir matriz de permisos para admin
      List<GroupRoleAssignment> adminPermissions = List.of(
          new GroupRoleAssignment("admin/super-admin",
              List.of("ADMIN_VIEW", "ADMIN_COMMAND", "CATALOG_CREATE", "CATALOG_ERASE")),
          new GroupRoleAssignment("admin/admin",
              List.of("ADMIN_VIEW", "ADMIN_COMMAND", "CATALOG_CREATE")),
          new GroupRoleAssignment("admin/editor",
              List.of("ADMIN_VIEW", "CATALOG_CREATE")));

      // Definir matriz de permisos para passenger
      List<GroupRoleAssignment> passengerPermissions = List.of(
          new GroupRoleAssignment("passenger/premium",
              List.of("CATALOG_VIEW", "FORM_COMPLETE")),
          new GroupRoleAssignment("passenger/standard",
              List.of("CATALOG_VIEW")),
          new GroupRoleAssignment("passenger/basic",
              List.of("CATALOG_VIEW")));

      // Asignar roles de admin a grupos
      System.out.println("IAM Bootstrap: Assigning admin permissions...");
      for (GroupRoleAssignment assignment : adminPermissions) {
        String groupId = groupIds.get(assignment.groupPath());
        if (groupId != null) {
          for (String roleName : assignment.roles()) {
            clientService.assingClientRoleToGroup(groupId, clientId, roleName);
            System.out.println("  ✓ Assigned " + roleName + " to " + assignment.groupPath());
          }
        } else {
          System.err.println("  ✗ Group not found: " + assignment.groupPath());
        }
      }

      // Asignar roles de passenger a grupos
      System.out.println("IAM Bootstrap: Assigning passenger permissions...");
      for (GroupRoleAssignment assignment : passengerPermissions) {
        String groupId = groupIds.get(assignment.groupPath());
        if (groupId != null) {
          for (String roleName : assignment.roles()) {
            clientService.assingClientRoleToGroup(groupId, clientId2, roleName);
            System.out.println("  ✓ Assigned " + roleName + " to " + assignment.groupPath());
          }
        } else {
          System.err.println("  ✗ Group not found: " + assignment.groupPath());
        }
      }
    }
    // CreateUserCommand cmd = CreateUserCommand.fromWebhook2("urgosxd@gmail.com",
    // "4421210", "ADMIN", "SUPER_ADMIN");
    // userService.register(cmd);

    System.out.println("IAM Bootstrap: Initialization complete");
  }
}
