package com.microservice.quarkus.user.identity.infrastructure.webhook.controler;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.GroupIdentityProvider;
import com.microservice.quarkus.user.identity.application.api.RoleIdentityProvider;
import com.microservice.quarkus.user.identity.application.dto.CreateUserCommand;
import com.microservice.quarkus.user.identity.application.dto.UserType;
import com.microservice.quarkus.user.identity.application.service.UserService;
import com.microservice.quarkus.user.identity.infrastructure.webhook.controler.dto.PayloadDTO;
import com.microservice.quarkus.user.shared.infrastructure.eventbus.CorrelationIdInterceptor;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/webhooks/payload")
public class PayloadCreateWebhookController {

  private static final Logger log = LoggerFactory.getLogger(PayloadCreateWebhookController.class);

  private static final Map<String, Set<String>> CLIENT_DEFAULT_ROLES = Map.of(
      "dashboard-client", Set.of("editor"),
      "frontend-client", Set.of("basic"));

  private static final Map<String, String> COMPOSITE_ROLE_TO_GROUP = Map.of(
      "super-admin", "admins",
      "admin", "admins",
      "editor", "admins",
      "premium", "passengers",
      "standard", "passengers",
      "basic", "passengers");

  private static final Map<String, String> COMPOSITE_ROLE_TO_CLIENT_ID = Map.of(
      "super-admin", "dashboard-client",
      "admin", "dashboard-client",
      "editor", "dashboard-client",
      "premium", "frontend-client",
      "standard", "frontend-client",
      "basic", "frontend-client");

  private final UserService userService;
  private final ClientIdentityProvider clientService;
  private final GroupIdentityProvider groupIdentityProvider;
  private final RoleIdentityProvider roleIdentityProvider;
  private final CorrelationIdInterceptor correlationIdInterceptor;

  @ConfigProperty(name = "webhook.payload.secret")
  String webhookSecret;

  @Inject
  public PayloadCreateWebhookController(UserService userService,
                                         ClientIdentityProvider clientService,
                                         GroupIdentityProvider groupIdentityProvider,
                                         RoleIdentityProvider roleIdentityProvider,
                                         CorrelationIdInterceptor correlationIdInterceptor) {
    this.userService = userService;
    this.clientService = clientService;
    this.groupIdentityProvider = groupIdentityProvider;
    this.roleIdentityProvider = roleIdentityProvider;
    this.correlationIdInterceptor = correlationIdInterceptor;
  }

  public record PayloadConsumerDTO(String providerid) {
  }

  @POST
  @PermitAll
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response receiveEvent(PayloadDTO dto, @Context HttpHeaders headers) {

    log.info("Webhook received: {}", (dto != null ? dto.getEmail() : "null"));

    String receivedSecret = headers.getHeaderString("X-Webhook-Secret");
    if (receivedSecret == null || !receivedSecret.equals(webhookSecret)) {
      log.info("Invalid webhook secret");
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("{\"error\": \"Invalid webhook secret\"}")
          .build();
    }

    if (dto == null || dto.getEmail() == null) {
      return Response.accepted().build();
    }

    try {
      String correlationId = correlationIdInterceptor.getOrCreateCorrelationId();

      Set<String> roles;
      String specificRole = dto.getRole();
      if (specificRole != null && !specificRole.isBlank()) {
        roles = Set.of(specificRole);
      } else {
        roles = CLIENT_DEFAULT_ROLES.get(clientService.getClientNameById(dto.getClientId().toString()));
        if (roles == null) {
          roles = Set.of("basic");
        }
      }

      String userType = UserType.fromCompositeRoles(roles).name();
      CreateUserCommand command = CreateUserCommand.fromWebhook2(dto.getEmail(), dto.getPassword(), userType, roles);
      String externalId = userService.register(command, correlationId);

      if (externalId != null) {
        assignGroupsAndRoles(externalId, roles);
      }

      return Response.ok(new PayloadConsumerDTO(externalId)).build();

    } catch (Exception e) {
      throw e;
    }
  }

  private void assignGroupsAndRoles(String externalId, Set<String> roles) {
    for (String role : roles) {
      String groupPath = COMPOSITE_ROLE_TO_GROUP.get(role);
      if (groupPath != null) {
        String groupId = groupIdentityProvider.findGroupByPath(groupPath);
        if (groupId != null) {
          groupIdentityProvider.assignUserToGroup(externalId, groupId);
          log.info("Usuario {} asignado al grupo {}", externalId, groupPath);
        } else {
          log.warn("Grupo no encontrado: {}", groupPath);
        }
      }

      String targetClientId = COMPOSITE_ROLE_TO_CLIENT_ID.get(role);
      if (targetClientId != null) {
        try {
          roleIdentityProvider.assignClientRoleToUser(externalId, targetClientId, role);
          log.info("Rol '{}' asignado al usuario {}", role, externalId);
        } catch (Exception e) {
          log.warn("Error asignando rol '{}' al usuario {}: {}", role, externalId, e.getMessage());
        }
      }
    }
  }
}
