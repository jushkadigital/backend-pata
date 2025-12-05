package com.microservice.quarkus.user.iam.infrastructure.rest.api;

import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.application.api.UserApiService;
import com.microservice.quarkus.user.iam.application.service.UserService;
import com.microservice.quarkus.user.iam.infrastructure.rest.api.UsersAPI;
import com.microservice.quarkus.user.iam.infrastructure.rest.dto.UserDTO;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.infrastructure.rest.dto.ResponseMessageDTO;

@ApplicationScoped
public class UserResource implements UsersAPI {

  @Inject
  UserService userService;

  @Inject
  IdentityProvider keycloakService;

  @Override
  public Response getAllUsers() {
    Response response;

    System.out.println("GETALLL");

    response = Response.noContent().build();

    return response;
  }

  @Override
  public Response getUserById(UUID id) {
    Response response;

    response = Response.noContent().build();

    return response;
  }

  @Override
  public Response createUser(UserDTO loanDTO) {
    Response response;

    response = Response.noContent().build();

    return response;

  }

  @Override
  public Response updateUserById(UUID id, UserDTO loanDTO) {
    Response response;

    response = Response.noContent().build();

    return response;

  }

  @Override
  public Response deleteUserById(UUID id) {
    Response response;

    response = Response.noContent().build();

    return response;

  }

}
