package com.microservice.quarkus.user.iam.application.service;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.domain.Role;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserRepository;
import com.microservice.quarkus.user.iam.domain.UserType;

import io.smallrye.common.annotation.Blocking;

@Path("/keycloak")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class KeycloakResource {

  @Inject
  UserService userService;
  @Inject
  UserRepositoryImpl userRepository;

  @Inject
  RoleService roleService;
  @Inject
  RoleRepositoryImpl roleRepository;

  @GET
  @Path("/users")
  public List<String> getUsers() {
    return userRepository.getAll().stream().map(User::getEmail).map(c -> c.value()).collect(Collectors.toList());
  }

  @GET
  @Path("/usersk")
  public List<String> getUsersK() {
    return userService.allUsersKeylcloak().stream().map(User::getEmail).map(c -> c.value())
        .collect(Collectors.toList());
  }

  @GET
  @Path("/rolesk")
  public List<String> getRolesK() {
    return roleService.allRolesKeycloak().stream().map(Role::getName)
        .collect(Collectors.toList());
  }

  @GET
  @Path("/users/create/{emailparam}")
  public String createUser(@PathParam("emailparam") String emailparam) {
    String password = "aoeaoe";
    String email = emailparam + "@oeuoeu.com";
    return userService.register(email, password, UserType.PASSENGER);
  }

  @GET
  @Path("/roles")
  public List<String> getRoles() {
    return roleRepository.getAll().stream().map(Role::getName).collect(Collectors.toList());
  }

  @GET
  @Path("/roles/{id}")
  public List<String> getRoles(@PathParam("id") String clientId) {
    return roleRepository.getAllById(clientId).stream().map(Role::getName).collect(Collectors.toList());
  }

  @GET
  @Path("/gettoken")
  public String obtenerTokenAdmin() {
    String token = userService.getToken();
    return token;
  }

}

// Clase DTO simple para la request de creación (ajusta según necesidades)
class CreateUserRequest {
  private String username;
  private String password;

  // Getters y setters
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
