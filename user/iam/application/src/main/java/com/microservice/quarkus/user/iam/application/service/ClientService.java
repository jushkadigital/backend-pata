package com.microservice.quarkus.user.iam.application.service;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.domain.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ClientService {

  @Inject
  IdentityProvider keycloakClient;

  @Transactional
  public String register(String name) {

    try {
      return keycloakClient.createClient(name);
    } catch (Exception e) {
      System.out.println("PIPIPI");
    }

    return "";
  }

  @Transactional
  public void assingClientRoleToGroup(String groupName, String clientId, String roleName) {
    keycloakClient.assingClientRoleToGroup(groupName, clientId, roleName);
  }

  public String getToken() {
    return keycloakClient.getToken();
  }

  public String getRealm() {
    return keycloakClient.getRealm();
  }

  public void configurarWebhook(String url) {
    keycloakClient.configurarWebhook(url);
  }

}
