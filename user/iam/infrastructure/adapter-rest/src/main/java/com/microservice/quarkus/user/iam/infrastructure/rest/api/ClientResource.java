package com.microservice.quarkus.user.iam.infrastructure.rest.api;

import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.application.api.UserApiService;
import com.microservice.quarkus.user.iam.application.service.UserService;
import com.microservice.quarkus.user.iam.infrastructure.rest.api.ClientsAPI;
import com.microservice.quarkus.user.iam.infrastructure.rest.dto.UserDTO;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.infrastructure.rest.dto.ResponseMessageDTO;

@ApplicationScoped
public class ClientResource implements ClientsAPI {

  @Inject
  UserService userService;

  @Inject
  IdentityProvider keycloakService;

  @Override
  public Response getClients() {
    Response response;

    response = Response.noContent().build();

    var clients = (keycloakService.getClientsCreatedByMe());
    if (null == clients) {
      response = Response.status(Response.Status.NOT_FOUND).build();
    } else {
      response = Response.ok(clients).build();
    }
    return response;
  }

}
