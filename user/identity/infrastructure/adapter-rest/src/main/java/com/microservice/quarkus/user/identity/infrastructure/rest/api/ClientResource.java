package com.microservice.quarkus.user.identity.infrastructure.rest.api;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.infrastructure.rest.api.ClientsAPI;

@ApplicationScoped
public class ClientResource implements ClientsAPI {

  @Inject
  ClientIdentityProvider keycloakService;

  @Override
  @PermitAll
  public Response getClients() {
    Response response;

    response = Response.noContent().build();

    var clients = keycloakService.getClientSummaries();
    if (null == clients) {
      response = Response.status(Response.Status.NOT_FOUND).build();
    } else {
      response = Response.ok(clients).build();
    }
    return response;
  }

}
