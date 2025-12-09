package com.microservice.quarkus.user.iam.infrastructure.webhook.controler;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.quarkus.user.iam.infrastructure.webhook.controler.dto.KeycloakDTO;
import com.microservice.quarkus.user.iam.infrastructure.webhook.event.WebhookPayload;

import io.vertx.mutiny.core.eventbus.EventBus; // Importante: versión Mutiny
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("/webhooks/keycloak")
public class KeycloakWebhookController {

  @Inject
  EventBus eventBus;

  @Inject
  ObjectMapper mapper;

  @POST
  public Response receiveEvent(KeycloakDTO dto) {

    System.out.println("Webhook received: " + (dto != null ? dto.getType() : "null"));

    if (dto == null || dto.getType() == null) {
      return Response.accepted().build();
    }

    List<String> eventsAllowed = List.of("REGISTER");

    if (!eventsAllowed.contains(dto.getType())) {
      return Response.accepted().build();
    }
    // 1. Convertir DTO externo a Payload interno
    WebhookPayload payload = new WebhookPayload(
        dto.getType(),
        dto.getUserId().toString(),
        dto.getClientId().toString(),
        mapper.convertValue(dto.getDetails(), new TypeReference<Map<String, String>>() {
        }));

    // 2. Despachar al Bus (Fire and Forget)
    // "iam.webhook.keycloak" es la dirección del evento
    eventBus.send("iam.webhook.keycloak", payload);

    // 3. Responder rápido. Keycloak queda feliz.
    return Response.accepted().build();
  }
}
