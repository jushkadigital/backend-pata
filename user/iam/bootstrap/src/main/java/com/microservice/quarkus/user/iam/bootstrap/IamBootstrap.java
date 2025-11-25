package com.microservice.quarkus.user.iam.bootstrap;

import java.util.List;

import com.microservice.quarkus.user.iam.application.service.ClientService;
import com.microservice.quarkus.user.iam.application.service.RoleService;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Startup
@ApplicationScoped
public class IamBootstrap {

  @Inject
  RoleService roleService;

  @Inject
  ClientService clientService;

  @PostConstruct
  void init() {
    System.out.println("INICIANDOOOOOOOOOOOOO");

    clientService.getRealm();

    System.out.println("Realm creado");

    clientService.configurarWebhook("https://webhook.site/7b00dbf6-a71a-4712-b144-ea3c0355fed9");

    clientService.getToken();

    String idOne = clientService.register("dashboard-client");
    System.out.println(idOne);

    List<String> roles = List.of("CATALOG_VIEW", "CATALOG_COMMAND", "CATALOG_DELETE", "CATALGO_ERASE");
    for (String role : roles) {
      roleService.register(role, "GGA OE OEOE OO", idOne);
    }

    clientService.assingClientRoleToGroup("admin", idOne, "CATALOG_VIEW");
    System.out.println("IamBootstrap CARGA DE ROLES COMPLETA");
  }
}
