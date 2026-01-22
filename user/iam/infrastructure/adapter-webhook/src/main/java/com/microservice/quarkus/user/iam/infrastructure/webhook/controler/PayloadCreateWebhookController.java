package com.microservice.quarkus.user.iam.infrastructure.webhook.controler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.application.dto.CreateUserCommand;
import com.microservice.quarkus.user.iam.application.service.UserService;
import com.microservice.quarkus.user.iam.infrastructure.webhook.controler.dto.PayloadDTO;
import com.microservice.quarkus.user.iam.infrastructure.webhook.event.WebhookPayloadPayload;

import io.vertx.mutiny.core.eventbus.EventBus; // Importante: versión Mutiny
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/webhooks/payload")
public class PayloadCreateWebhookController {
  @Inject
  UserService userService;

  @Inject
  ObjectMapper mapper;

  @ConfigProperty(name = "webhook.payload.secret")
  String webhookSecret;

  @Inject
  IdentityProvider clientService;

  private final Map<String, String> cliNameDict = new HashMap<>() {
    {
      put("dashboard-client", "ADMIN");
      put("frontend-client", "PASSENGER");
    }
  };

  public record PayloadConsumerDTO(String providerid) {

  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response receiveEvent(PayloadDTO dto, @Context HttpHeaders headers) {

    System.out.println("Webhook received: " + (dto != null ? dto.getType() : "null"));

    // Validar secret
    String receivedSecret = headers.getHeaderString("X-Webhook-Secret");
    if (receivedSecret == null || !receivedSecret.equals(webhookSecret)) {
      System.out.println("Invalid webhook secret");
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("{\"error\": \"Invalid webhook secret\"}")
          .build();
    }

    if (dto == null || dto.getType() == null) {
      return Response.accepted().build();
    }

    // 1. Convertir DTO externo a Payload interno
    WebhookPayloadPayload payload = new WebhookPayloadPayload(
        dto.getEmail(),
        dto.getPassword(),
        dto.getType(),
        dto.getClientId().toString());

    try {
      // Lógica de filtrado
      CreateUserCommand command = CreateUserCommand.fromWebhook2(payload.email(), payload.password(),
          cliNameDict.get(clientService.getClientNameById(payload.clientId())), payload.type());
      String providerId = userService.register(command);
      return Response.ok(new PayloadConsumerDTO(providerId)).build();

    } catch (Exception e) {
      // Si lanzas excepción aquí, @Retry se activa.
      throw e;
    }

    // 3. Responder rápido. Keycloak queda feliz.
  }
}
