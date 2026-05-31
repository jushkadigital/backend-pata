package com.microservice.quarkus.user.identity.infrastructure.rest.api;

import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.application.service.UserService;
import com.microservice.quarkus.user.identity.infrastructure.rest.api.ClientsAPI;
import com.microservice.quarkus.user.identity.infrastructure.rest.dto.UserDTO;
import com.microservice.quarkus.user.identity.infrastructure.rest.dto.ResponseMessageDTO;
import jakarta.ws.rs.Path;

@ApplicationScoped
public class ClientResource implements ClientsAPI {

  @Inject
  UserService userService;

  @Inject
  ClientIdentityProvider keycloakService;

  @Override
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
