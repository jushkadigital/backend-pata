package com.microservice.quarkus.user.iam.bootstrap;

import java.util.List;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.application.service.RoleService;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class IamBootstrap {

  private static final int MAX_RETRIES = 30;
  private static final int RETRY_DELAY_MS = 3000;

  @Inject
  RoleService roleService;

  @Inject
  IdentityProvider clientService;

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

    clientService.configurarWebhook("http://172.17.0.1:8081/webhooks/keycloak");
    clientService.getToken("http://172.17.0.1:8081/webhooks/keycloak");

    String clientId = clientService.createClient("dashboard-client");
    System.out.println("IAM Bootstrap: Client created with ID: " + clientId);

    String clientId2 = clientService.createClient("frontend-client");
    System.out.println("IAM Bootstrap: Client created with ID: " + clientId2);

    if (clientId != null && !clientId.isEmpty() && clientId2 != null && !clientId2.isEmpty()) {
      List<String> roles = List.of("CATALOG_VIEW", "CATALOG_COMMAND", "CATALOG_DELETE", "CATALOG_ERASE");
      for (String role : roles) {
        roleService.register(role, "Catalog role", clientId);
        roleService.register(role, "Catalog role", clientId2);
      }

      // clientService.assingClientRoleToGroup("admin", clientId, "CATALOG_VIEW");
    }

    System.out.println("IAM Bootstrap: Initialization complete");
  }
}
