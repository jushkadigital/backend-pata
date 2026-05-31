package com.microservice.quarkus.user.identity.infrastructure.rest.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.stream.Collectors;

import com.microservice.quarkus.user.identity.application.api.IdentitySyncRepository;
import com.microservice.quarkus.user.identity.application.api.RoleSyncRepository;
import com.microservice.quarkus.user.identity.application.dto.KeycloakRoleDTO;
import com.microservice.quarkus.user.identity.application.dto.KeycloakUserDTO;
import com.microservice.quarkus.user.identity.application.service.UserService;
import com.microservice.quarkus.user.identity.application.service.RoleService;

import io.smallrye.common.annotation.Blocking;

@Path("/keycloak")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class KeycloakResource {

  @Inject
  UserService userService;

  @Inject
  RoleService roleService;

  @Inject
  IdentitySyncRepository identitySyncRepository;

  @Inject
  RoleSyncRepository roleSyncRepository;

  @GET
  @Path("/users")
  public List<String> getUsers() {
    return identitySyncRepository.findAll().stream()
        .map(r -> r.email())
        .collect(Collectors.toList());
  }

  @GET
  @Path("/usersk")
  public List<String> getUsersK() {
    return userService.allUsersKeycloak().stream()
        .map(KeycloakUserDTO::email)
        .collect(Collectors.toList());
  }

  @GET
  @Path("/rolesk")
  public List<String> getRolesK() {
    return roleService.allRolesKeycloak().stream()
        .map(KeycloakRoleDTO::name)
        .collect(Collectors.toList());
  }

  @GET
  @Path("/roles")
  public List<String> getRoles() {
    return roleSyncRepository.findAll().stream()
        .map(r -> r.name())
        .collect(Collectors.toList());
  }

  @GET
  @Path("/roles/{id}")
  public List<String> getRoles(@PathParam("id") String clientId) {
    return roleSyncRepository.findByClientId(clientId).stream()
        .map(r -> r.name())
        .collect(Collectors.toList());
  }

}
