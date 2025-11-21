package com.microservice.quarkus.user.iam.infrastructure.keycloak;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.management.relation.Role;

import com.microservice.quarkus.user.iam.domain.UserType;

@ApplicationScoped
public class KeycloakService {

  @Inject
  Keycloak keycloak;

  public static final String REALM_NAME = "quarkus";

  public List<UserRepresentation> getUsers() {
    return keycloak.realm(REALM_NAME).users().list();
  }

  public List<RoleRepresentation> getRoles() {
    return keycloak.realm(REALM_NAME).roles().list();

  }

  public String getGroupById(String id) {
    return keycloak.realm("quarkus").users().get(id).groups().stream().map(GroupRepresentation::getName).findFirst()
        .map(Object::toString).orElse("NO_GROUP");
  }

  public String createUser(String email, String password, UserType roleType) {

    UsersResource userResource = keycloak.realm(REALM_NAME).users();
    List<UserRepresentation> existingUsers = userResource.searchByUsername(email, true);

    if (!existingUsers.isEmpty()) {

      return "";
    } else {

      UserRepresentation user = new UserRepresentation();

      user.setEmail(email);
      user.setEnabled(true);

      if (roleType == UserType.ADMIN) {
        user.setGroups(List.of("admin"));
      } else {
        user.setGroups(List.of("passenger"));
      }

      CredentialRepresentation credential = new CredentialRepresentation();
      credential.setType("password");
      credential.setValue(password);
      credential.setTemporary(false);
      user.setCredentials(Collections.singletonList(credential));

      Response response = keycloak.realm(REALM_NAME).users().create(user);

      try {
        int status = response.getStatus();

        if (status == 201) {
          String locationHeader = response.getHeaderString("Location");

          if (locationHeader != null) {
            // ✅ PRÁCTICA 2: Encapsular la lógica de extracción del ID.
            return extractIdFromLocation(locationHeader);
          } else {
            // Manejo si el 201 no incluye el Location, aunque es poco común en Keycloak
            System.err.println("Advertencia: Status 201 sin encabezado 'Location'.");
            return null;

          }
        } else {

          String errorMessage = response.hasEntity()
              ? response.readEntity(String.class)
              : "No hay mensaje de error disponible.";

          System.err.printf("Error al crear usuario. Status: %d. Mensaje: %s%n", status, errorMessage);
          // Aquí puedes lanzar una excepción personalizada si lo deseas
          return null;
        }
      } catch (Exception e) {
        // Manejo de la excepción si la lectura falla
        e.printStackTrace();
        return null;
      }
    }

  }

  private static String extractIdFromLocation(String locationUrl) {
    if (locationUrl == null || locationUrl.isEmpty()) {
      return null;
    }
    // Divide la URL por el caracter '/'
    String[] parts = locationUrl.split("/");

    // El ID será el último elemento del array
    if (parts.length > 0) {
      return parts[parts.length - 1];
    }
    return null;
  }

  public String createClient(String name) {
    ClientRepresentation clientRep = new ClientRepresentation();
    clientRep.setClientId(UUID.randomUUID().toString());
    clientRep.setName(name);
    clientRep.setEnabled(true);
    clientRep.setDirectAccessGrantsEnabled(true);
    clientRep.setPublicClient(true);
    clientRep.setSecret("admin");

    clientRep.setRedirectUris(Arrays.asList("http://localhost:8081/*"));

    Response response = keycloak.realm(REALM_NAME).clients().create(clientRep);
    if (response.getStatus() == 201) {
      String clientUuid = CreatedResponseUtil.getCreatedId(response);
      System.out.println("Cliente creado con UUID: " + clientUuid);
      return clientUuid;
    } else {
      System.out.println("Error " + response.getStatus());

    }
    response.close();
    return "";

  }

  public String findOrCreateRealmRole(String roleName, String description, String clientId) {
    System.out.println("ENTREEE");

    RolesResource rolesResource = keycloak.realm(REALM_NAME).clients().get(clientId).roles();
    // Intenta obtener el rol (si ya existe)
    RoleRepresentation existingRole = null;
    try {
      existingRole = rolesResource.get(roleName).toRepresentation();
    } catch (Exception e) {
      // Capturamos Exception genérica temporalmente para ver qué está pasando
      // realmente
      // Si es 404, e será NotFoundException (Jakarta)
    }
    if (existingRole != null) {
      System.out.println("Ya Existe");
      return ""; // O devuelve existingRole.getName();
    }

    System.out.println("AL OTRO LADO");
    RoleRepresentation role = new RoleRepresentation();
    role.setName(roleName);
    role.setDescription(description);
    keycloak.realm(REALM_NAME).roles().create(role);
    System.out.println("Rol de Realm '" + roleName + "' creado.");
    // Lo recupera después de crearlo para devolver la representación completa (con
    // ID)
    return roleName;
  }

}
