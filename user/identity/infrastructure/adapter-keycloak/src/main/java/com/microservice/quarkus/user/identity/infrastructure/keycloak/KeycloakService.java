package com.microservice.quarkus.user.identity.infrastructure.keycloak;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jboss.resteasy.reactive.ClientWebApplicationException;
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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.quarkus.user.identity.application.dto.ClientSummary;
import com.microservice.quarkus.user.identity.application.dto.TenantConfigDTO;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationScoped
public class KeycloakService {

  private static final Logger log = LoggerFactory.getLogger(KeycloakService.class);

  @Inject
  Keycloak keycloak;

  @Inject
  MeterRegistry meterRegistry;

  private Timer timer;

  @ConfigProperty(name = "quarkus.keycloak.admin-client.server-url", defaultValue = "http://localhost:8080")
  String keycloakServerUrl;

  @PostConstruct
  void init() {
    log.info("KeycloakService initialized with server-url: {}", keycloakServerUrl);
    timer = Timer.builder("keycloak.request.duration")
        .description("Keycloak HTTP request duration")
        .register(meterRegistry);
  }

  public static final String REALM_NAME = "quarkus";

  public List<UserRepresentation> getUsers() {
    return keycloak.realm(REALM_NAME).users().list();
  }

  public UserRepresentation getUserById(String id) {
    return keycloak.realm(REALM_NAME).users().get(id).toRepresentation();
  }

  public List<RoleRepresentation> getRoles() {
    return keycloak.realm(REALM_NAME).roles().list();

  }

  public String getGroupById(String id) {
    return keycloak.realm("quarkus").users().get(id).groups().stream().map(GroupRepresentation::getName).findFirst()
        .map(Object::toString).orElse("NO_GROUP");
  }

  @WithSpan("keycloak.user.create")
  public String createUser(String email, String password) {
    try {
      return timer.recordCallable(() -> {
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
                return extractIdFromLocation(locationHeader);
              } else {
                log.error("Advertencia: Status 201 sin encabezado 'Location'.");
                return null;

              }
            } else {

              String errorMessage = response.hasEntity()
                  ? response.readEntity(String.class)
                  : "No hay mensaje de error disponible.";

              log.error("Error al crear usuario. Status: {}. Mensaje: {}", status, errorMessage);
              return null;
            }
          } catch (Exception e) {
            log.error("Error al crear usuario", e);
            return null;
          }
        }
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String extractIdFromLocation(String locationUrl) {
    if (locationUrl == null || locationUrl.isEmpty()) {
      return null;
    }
    String[] parts = locationUrl.split("/");

    if (parts.length > 0) {
      return parts[parts.length - 1];
    }
    return null;
  }

  public String createClient(String name, List<String> items) {
    List<String> result = new ArrayList<>();
    result.add("http://localhost:8081/*");

    result.addAll(items);
    ClientRepresentation clientRep = new ClientRepresentation();
    clientRep.setName(name);
    clientRep.setEnabled(true);

    clientRep.setPublicClient(false);
    clientRep.setSecret(UUID.randomUUID().toString());

    clientRep.setServiceAccountsEnabled(true);
    clientRep.setStandardFlowEnabled(true);
    clientRep.setDirectAccessGrantsEnabled(true);

    clientRep.setRedirectUris(result);

    Map<String, String> atributos = new HashMap<>();

    for (String item : items) {
      atributos.put("post.logout.redirect.uris", item);
    }

    clientRep.setAttributes(atributos);

    Response response = keycloak.realm(REALM_NAME).clients().create(clientRep);
    if (response.getStatus() == 201) {
      String clientUuid = CreatedResponseUtil.getCreatedId(response);
      log.info("Cliente creado con UUID: {}", clientUuid);
      return clientUuid;
    } else {
      log.error("Error {}", response.getStatus());
    }
    response.close();
    return "";

  }

  public record ClientInfo(String id, String secret) {
  }

  public Map<String, ClientInfo> getClientsCreatedByMe() {
    List<ClientRepresentation> myClients = keycloak.realm(REALM_NAME).clients().findAll(null, true, true, 0, 100);
    return myClients.stream()
        .filter(c -> c.getName() != null && c.getName().toLowerCase().contains("client".toLowerCase()))
        .collect(Collectors.toMap(
            ClientRepresentation::getName,
            c -> {
              String secret = keycloak.realm(REALM_NAME)
                  .clients()
                  .get(c.getId())
                  .getSecret()
                  .getValue();
              return new ClientInfo(c.getId(), secret);
            }));
  }

  public List<ClientSummary> getClientSummaries() {
    List<ClientRepresentation> myClients = keycloak.realm(REALM_NAME).clients().findAll(null, true, true, 0, 100);
    return myClients.stream()
        .filter(c -> c.getName() != null && c.getName().toLowerCase().contains("client".toLowerCase()))
        .map(c -> new ClientSummary(c.getId(), c.getName()))
        .collect(Collectors.toList());
  }

  public TenantConfigDTO getTenantConfig(String clientName) {
    Map<String, ClientInfo> clients = getClientsCreatedByMe();
    ClientInfo clientInfo = clients.get(clientName);

    if (clientInfo == null) {
      return null;
    }

    return new TenantConfigDTO(
        clientInfo.id(),
        clientInfo.secret(),
        "http://localhost:8080/realms/" + REALM_NAME);
  }

  public String getClientNameById(String clientId) {
    try {
      ClientRepresentation clientRep = keycloak.realm(REALM_NAME)
          .clients()
          .get(clientId)
          .toRepresentation();

      return clientRep.getName();

    } catch (NotFoundException e) {
      return "Cliente no encontrado";
    } catch (Exception e) {
      throw new RuntimeException("Error al conectar con Keycloak", e);
    }
  }

  public void assignClientRoleToGroup(String groupId, String clientId, String roleName) {
    log.debug("ENTRA ASSAING");
    RealmResource realm = keycloak.realm(REALM_NAME);
    try {
      List<ClientRepresentation> clients = realm.clients().findByClientId(clientId);
      if (clients.isEmpty()) {
        throw new RuntimeException("El cliente '" + clientId + "' no existe.");
      }
      String clientUuid = clients.get(0).getId();

      log.debug("Client UUID: {}", clientUuid);

      RoleRepresentation clientRole = realm.clients().get(clientUuid)
          .roles().get(roleName).toRepresentation();

      log.debug("Role found: {}", roleName);

      realm.groups().group(groupId)
          .roles()
          .clientLevel(clientUuid)
          .add(Collections.singletonList(clientRole));

      log.info("ÉXITO: Rol '{}' (del cliente {}) asignado al grupo ID: {}", roleName, clientId, groupId);

    } catch (Exception e) {
      log.error("ERROR asignando rol: {}", e.getMessage(), e);
    }
  }

  public void assignClientRoleToUser(String userId, String clientId, String roleName) {
    RealmResource realm = keycloak.realm(REALM_NAME);
    try {
      List<ClientRepresentation> clients = realm.clients().findByClientId(clientId);
      if (clients.isEmpty()) {
        throw new RuntimeException("El cliente '" + clientId + "' no existe.");
      }
      String clientUuid = clients.get(0).getId();

      RoleRepresentation clientRole = realm.clients().get(clientUuid)
          .roles().get(roleName).toRepresentation();

      realm.users().get(userId)
          .roles()
          .clientLevel(clientUuid)
          .add(Collections.singletonList(clientRole));

      log.info("Rol '{}' (del cliente {}) asignado al usuario ID: {}", roleName, clientId, userId);

    } catch (Exception e) {
      log.error("ERROR asignando rol al usuario: {}", e.getMessage(), e);
      throw new RuntimeException("Error asignando rol '" + roleName + "' al usuario " + userId, e);
    }
  }

  public String createCompositeClientRole(String roleName, String description, String clientId, List<String> compositeRoleNames) {
    RealmResource realm = keycloak.realm(REALM_NAME);
    try {
      List<ClientRepresentation> clients = realm.clients().findByClientId(clientId);
      if (clients.isEmpty()) {
        throw new RuntimeException("El cliente '" + clientId + "' no existe.");
      }
      String clientUuid = clients.get(0).getId();

      RolesResource rolesResource = realm.clients().get(clientUuid).roles();

      RoleRepresentation existingRole = null;
      try {
        existingRole = rolesResource.get(roleName).toRepresentation();
      } catch (Exception e) {
      }

      if (existingRole != null) {
        log.info("Rol compuesto '{}' ya existe en el cliente {}", roleName, clientId);
        return "";
      }

      RoleRepresentation compositeRole = new RoleRepresentation();
      compositeRole.setName(roleName);
      compositeRole.setDescription(description);
      compositeRole.setComposite(true);
      rolesResource.create(compositeRole);

      List<RoleRepresentation> composites = new ArrayList<>();
      for (String compositeName : compositeRoleNames) {
        try {
          RoleRepresentation childRole = rolesResource.get(compositeName).toRepresentation();
          composites.add(childRole);
        } catch (Exception e) {
          log.warn("Rol hijo '{}' no encontrado en el cliente {}, omitiendo del composite", compositeName, clientId);
        }
      }

      if (!composites.isEmpty()) {
        rolesResource.get(roleName).addComposites(composites);
      }

      log.info("Rol compuesto '{}' creado en el cliente {} con {} roles hijos", roleName, clientId, composites.size());
      return roleName;

    } catch (Exception e) {
      log.error("Error creando rol compuesto '{}': {}", roleName, e.getMessage(), e);
      throw new RuntimeException("Error creando rol compuesto: " + roleName, e);
    }
  }

  public String findOrCreateRealmRole(String roleName, String description, String clientId) {
    log.debug("ENTREEE");

    RolesResource rolesResource = keycloak.realm(REALM_NAME).clients().get(clientId).roles();
    RoleRepresentation existingRole = null;
    try {
      existingRole = rolesResource.get(roleName).toRepresentation();
    } catch (Exception e) {
    }
    if (existingRole != null) {
      log.debug("Ya Existe");
      return "";
    }

    log.debug("AL OTRO LADO");
    RoleRepresentation role = new RoleRepresentation();
    role.setName(roleName);
    role.setDescription(description);
    keycloak.realm(REALM_NAME).clients().get(clientId).roles().create(role);
    log.info("Rol de Realm '{}' creado.", roleName);
    return roleName;
  }

  public RealmRepresentation getRealm() {
    RealmRepresentation realm = new RealmRepresentation();

    realm.setRealm("quarkus");
    realm.setEnabled(true);
    realm.setUsers(new ArrayList<>());
    realm.setClients(new ArrayList<>());

    realm.setAccessTokenLifespan(600);
    realm.setSsoSessionMaxLifespan(600);
    realm.setRefreshTokenMaxReuse(10);
    realm.setRequiredActions(List.of());

    RolesRepresentation roles = new RolesRepresentation();
    List<RoleRepresentation> realmRoles = new ArrayList<>();

    realmRoles.add(new RoleRepresentation("user", null, false));
    realmRoles.add(new RoleRepresentation("admin", null, false));

    roles.setRealm(realmRoles);
    realm.setRoles(roles);

    realm.setRegistrationEmailAsUsername(true);
    realm.setRegistrationAllowed(true);

    realm.setEventsEnabled(true);
    realm.setEventsExpiration(2592000L);
    realm.setEventsListeners(List.of("jboss-logging", "ext-event-webhook", "ext-event-http", "observability-events"));

    try {
      keycloak.realms().create(realm);

    } catch (ClientWebApplicationException e) {
      if (e.getResponse().getStatus() == 409) {
        log.info("El Realm ya existe, omitiendo creación.");
      } else {
        throw e;
      }
    }
    return realm;
  }

  public String getToken(String uri) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      String token = keycloak.tokenManager().getAccessTokenString();

      Map<String, Object> webhookData = new HashMap<>();
      webhookData.put("enabled", "true");
      webhookData.put("url", uri);
      webhookData.put("secret", "secret");
      webhookData.put("eventTypes", List.of("REGISTER"));
      String jsonBody = objectMapper
          .writeValueAsString(webhookData);

      HttpClient client = HttpClient.newHttpClient();

      String keycloakBaseUrl = System.getenv("KEYCLOAK_INTERNAL_URL") != null ? System.getenv("KEYCLOAK_INTERNAL_URL") : keycloakServerUrl;
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(String.format("%s/realms/%s/webhooks",keycloakBaseUrl, REALM_NAME)))
          .header("Authorization", "Bearer " + token)
          .header("Content-Type", "application/json")
          .POST(BodyPublishers.ofString(jsonBody))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      log.info("Iniciando Webhooks: {}", response.body());

      HttpRequest request2 = HttpRequest.newBuilder()
          .uri(URI.create(String.format("%s/realms/%s/webhooks",keycloakBaseUrl, REALM_NAME)))
          .header("Authorization", "Bearer " + token)
          .GET()
          .build();

      HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());

      log.info("List Webhooks: {}", response2.body());
      return "GAAA";

    } catch (Exception e) {
      log.error("Error en getToken", e);
      return "GEEE";
    }
  }

  public void configurarWebhook(String targetUrl) {
    String realmName = REALM_NAME;
    String configKey = "_providerConfig.ext-event-http";

    Map<String, Object> configMap = new HashMap<>();
    configMap.put("targetUri", targetUrl);

    ObjectMapper mapper = new ObjectMapper();
    String jsonConfigValue;

    try {
      jsonConfigValue = mapper.writeValueAsString(configMap);
    } catch (Exception e) {
      throw new RuntimeException("Error creando JSON de configuración", e);
    }

    var realmResource = keycloak.realm(realmName);
    RealmRepresentation realmRep = realmResource.toRepresentation();

    Map<String, String> attributes = realmRep.getAttributes();
    if (attributes == null) {
      attributes = new HashMap<>();
    }

    attributes.put(configKey, jsonConfigValue);

    realmRep.setAttributes(attributes);

    realmResource.update(realmRep);

    log.info("Configuración de Webhook actualizada exitosamente.");
  }

  public Map<String, String> createGroupHierarchy(List<List<String>> groupHierarchies) {
    RealmResource realm = keycloak.realm(REALM_NAME);
    Map<String, String> createdGroups = new HashMap<>();

    for (List<String> hierarchy : groupHierarchies) {
      if (hierarchy == null || hierarchy.isEmpty()) {
        continue;
      }

      String parentGroupId = null;
      StringBuilder pathBuilder = new StringBuilder();

      for (int i = 0; i < hierarchy.size(); i++) {
        String groupName = hierarchy.get(i);

        if (groupName == null || groupName.trim().isEmpty()) {
          continue;
        }

        if (pathBuilder.length() > 0) {
          pathBuilder.append("/");
        }
        pathBuilder.append(groupName);
        String fullPath = pathBuilder.toString();

        String groupId = findGroupByNameAndParent(realm, groupName, parentGroupId);

        if (groupId == null) {
          GroupRepresentation newGroup = new GroupRepresentation();
          newGroup.setName(groupName);

          Response response;
          if (parentGroupId == null) {
            response = realm.groups().add(newGroup);
          } else {
            response = realm.groups().group(parentGroupId).subGroup(newGroup);
          }

          try {
            if (response.getStatus() == 201) {
              groupId = extractIdFromLocation(response.getHeaderString("Location"));
              createdGroups.put(fullPath, groupId);
              log.info("Grupo creado: {} (ID: {})", fullPath, groupId);
            } else {
              String errorMessage = response.hasEntity()
                  ? response.readEntity(String.class)
                  : "Error desconocido";
              log.error("Error al crear grupo '{}'. Status: {}. Mensaje: {}", fullPath, response.getStatus(), errorMessage);
              continue;
            }
          } finally {
            response.close();
          }
        } else {
          log.info("Grupo ya existe: {} (ID: {})", fullPath, groupId);
          createdGroups.put(fullPath, groupId);
        }

        parentGroupId = groupId;
      }
    }

    return createdGroups;
  }

  private String findGroupByNameAndParent(RealmResource realm, String groupName, String parentId) {
    log.debug("Desde findByNameAndParenst");
    log.debug("{}", groupName);
    try {
      List<GroupRepresentation> groups;

      if (parentId == null) {
        groups = realm.groups().groups(groupName, 0, 10, false);
      } else {
        groups = realm.groups().group(parentId).getSubGroups(0, 10, true);

      }

      return groups.stream()
          .filter(g -> g.getName().equals(groupName))
          .findFirst()
          .map(GroupRepresentation::getId)
          .orElse(null);

    } catch (Exception e) {
      log.error("Error buscando grupo '{}': {}", groupName, e.getMessage());
      return null;
    }
  }

  public String findGroupByPath(String groupPath) {
    RealmResource realm = keycloak.realm(REALM_NAME);
    String[] parts = groupPath.split("/");

    String currentParentId = null;

    for (String groupName : parts) {
      String groupId = findGroupByNameAndParent(realm, groupName, currentParentId);
      if (groupId == null) {
        log.error("No se encontró el grupo '{}' en la ruta '{}'", groupName, groupPath);
        return null;
      }
      currentParentId = groupId;
    }

    return currentParentId;
  }

  @WithSpan("keycloak.user.delete")
  public void deleteUser(String externalId) {
    timer.record(() -> {
      try {
        keycloak.realm(REALM_NAME).users().get(externalId).remove();
        log.info("Usuario {} eliminado de Keycloak (compensación)", externalId);
      } catch (Exception e) {
        log.warn("No se pudo eliminar usuario {} de Keycloak durante compensación: {}", externalId, e.getMessage());
      }
    });
  }

  public void assignUserToGroup(String userId, String groupId) {
    if (userId == null || groupId == null) {
      throw new IllegalArgumentException("UserId y GroupId son obligatorios");
    }

    RealmResource realm = keycloak.realm(REALM_NAME);
    try {
      realm.users()
          .get(userId)
          .joinGroup(groupId);

      log.info("Usuario {} agregado exitosamente al grupo {}", userId, groupId);

    } catch (Exception e) {
      log.error("Error al asignar grupo: {}", e.getMessage(), e);
      throw e;
    }
  }

}
