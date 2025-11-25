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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.management.relation.Role;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    clientRep.setName(name);
    clientRep.setEnabled(true);
    clientRep.setDirectAccessGrantsEnabled(true);
    clientRep.setPublicClient(true);
    clientRep.setSecret("admin");

    clientRep.setRedirectUris(Arrays.asList("http://localhost:8081/*", "https://jwt.io"));

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

  public void assignClientRoleToGroup(String groupName, String clientId, String roleName) {
    System.out.println("ENTRA ASSAING");
    RealmResource realm = keycloak.realm(REALM_NAME);
    try {
      // 1. BUSCAR EL GRUPO (Necesitamos su ID UUID)
      // .groups(search, first, max)
      List<GroupRepresentation> groups = realm.groups().groups(groupName, 0, 1);
      if (groups.isEmpty()) {
        throw new RuntimeException("El grupo '" + groupName + "' no existe.");
      }
      String groupUuid = groups.get(0).getId();

      // 2. BUSCAR EL CLIENTE (Necesitamos su ID UUID, no el clientId string)
      List<ClientRepresentation> clients = realm.clients().findByClientId(clientId);
      if (clients.isEmpty()) {
        throw new RuntimeException("El cliente '" + clientId + "' no existe.");
      }
      String clientUuid = clients.get(0).getId();

      System.out.println(clientUuid);

      System.out.println("ASSAING RO");
      // 3. BUSCAR EL ROL (Necesitamos el objeto Role completo)
      // Nota: Buscamos el rol DENTRO del cliente específico usando clientUuid
      RoleRepresentation clientRole = realm.clients().get(clientUuid)
          .roles().get(roleName).toRepresentation();

      System.out.println("PASEEE CLIENT ROL");
      // 4. HACER LA VINCULACIÓN FINAL
      // Entramos al grupo -> roles -> clientLevel(clientUuid) -> add
      realm.groups().group(groupUuid)
          .roles()
          .clientLevel(clientUuid)
          .add(Collections.singletonList(clientRole));

      System.out.println(
          "✅ ÉXITO: Rol '" + roleName + "' (del cliente " + clientId + ") asignado al grupo '" + groupName + "'");

    } catch (Exception e) {
      System.err.println("❌ ERROR asignando rol: " + e.getMessage());
      // Aquí podrías lanzar una excepción personalizada
    }
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
    keycloak.realm(REALM_NAME).clients().get(clientId).roles().create(role);
    System.out.println("Rol de Realm '" + roleName + "' creado.");
    // Lo recupera después de crearlo para devolver la representación completa (con
    // ID)
    return roleName;
  }

  public RealmRepresentation getRealm() {
    RealmRepresentation realm = new RealmRepresentation();

    realm.setRealm("quarkus"); // Nombre por defecto
    realm.setEnabled(true);
    realm.setUsers(new ArrayList<>());
    realm.setClients(new ArrayList<>());

    // Configuraciones de tiempo (Token Lifespan)
    realm.setAccessTokenLifespan(600);
    realm.setSsoSessionMaxLifespan(600);
    realm.setRefreshTokenMaxReuse(10);
    realm.setRequiredActions(List.of()); // Lista vacía

    // Configuración de Roles
    RolesRepresentation roles = new RolesRepresentation();
    List<RoleRepresentation> realmRoles = new ArrayList<>();

    // Agregamos los roles por defecto de Quarkus
    realmRoles.add(new RoleRepresentation("user", null, false));
    realmRoles.add(new RoleRepresentation("admin", null, false));

    roles.setRealm(realmRoles);
    realm.setRoles(roles);

    realm.setRegistrationEmailAsUsername(true);
    realm.setRegistrationAllowed(true);

    realm.setEventsEnabled(true);
    realm.setEventsExpiration(2592000L);
    realm.setEventsListeners(List.of("jboss-logging", "ext-event-webhook", "ext-event-http"));

    keycloak.realms().create(realm);
    return realm;
  }

  public String getToken() {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      String token = keycloak.tokenManager().getAccessTokenString();

      Map<String, Object> webhookData = new HashMap<>();
      webhookData.put("enabled", "true"); // String "true" según tu curl
      webhookData.put("url", "https://webhook.site/7b00dbf6-a71a-4712-b144-ea3c0355fed9");
      webhookData.put("secret", "secret");
      webhookData.put("eventTypes", List.of("REGISTER", "LOGIN", "UPDATE_PROFILE"));
      String jsonBody = objectMapper
          .writeValueAsString(webhookData);

      HttpClient client = HttpClient.newHttpClient();

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(String.format("http://localhost:8080/realms/%s/webhooks", REALM_NAME)))
          .header("Authorization", "Bearer " + token) // Aquí inyectas el token
          .header("Content-Type", "application/json")
          .POST(BodyPublishers.ofString(jsonBody))
          .build();

      // 3. EJECUTAR
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      System.out.println("Iniciando Webhooks: " + response.body());

      HttpRequest request2 = HttpRequest.newBuilder()
          .uri(URI.create(String.format("http://localhost:8080/realms/%s/webhooks", REALM_NAME)))
          .header("Authorization", "Bearer " + token) // Aquí inyectas el token
          .GET()
          .build();

      HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());

      System.out.println("List Webhooks: " + response2.body());
      return "GAAA";

    } catch (Exception e) {
      e.printStackTrace();
      return "GEEE";
    }
  }

  public void configurarWebhook(String targetUrl) {
    String realmName = REALM_NAME; // El nombre de tu realm
    String configKey = "_providerConfig.ext-event-http"; // La llave mágica del plugin

    // 1. Preparar la configuración como JSON String
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("targetUri", targetUrl);
    // Puedes agregar más opciones si el plugin lo requiere, ej: "includeEvents":
    // ["REGISTER"]

    ObjectMapper mapper = new ObjectMapper();
    String jsonConfigValue;

    try {
      jsonConfigValue = mapper.writeValueAsString(configMap);
    } catch (Exception e) {
      throw new RuntimeException("Error creando JSON de configuración", e);
    }

    // 2. Obtener el Realm Resource y su Representación
    var realmResource = keycloak.realm(realmName);
    RealmRepresentation realmRep = realmResource.toRepresentation();

    // 3. Obtener los atributos actuales (o crear mapa nuevo si es nulo)
    Map<String, String> attributes = realmRep.getAttributes();
    if (attributes == null) {
      attributes = new HashMap<>();
    }

    // 4. Insertar la configuración
    // OJO: Keycloak espera que el valor sea un String, por eso convertimos el JSON
    // a String arriba
    attributes.put(configKey, jsonConfigValue);

    realmRep.setAttributes(attributes);

    // 5. Actualizar el Realm (Esto guarda la configuración)
    realmResource.update(realmRep);

    System.out.println("Configuración de Webhook actualizada exitosamente.");
  }

}
