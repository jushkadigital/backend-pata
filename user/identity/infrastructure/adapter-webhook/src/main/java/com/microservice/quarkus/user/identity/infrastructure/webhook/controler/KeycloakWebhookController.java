package com.microservice.quarkus.user.identity.infrastructure.webhook.controler;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.quarkus.user.identity.infrastructure.webhook.controler.dto.KeycloakDTO;
import com.microservice.quarkus.user.identity.infrastructure.webhook.event.WebhookKeycloakPayload;

import io.vertx.mutiny.core.eventbus.EventBus; // Importante: versión Mutiny
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/webhooks/keycloak")
public class KeycloakWebhookController {

  private static final Logger log = LoggerFactory.getLogger(KeycloakWebhookController.class);

  @Inject
  EventBus eventBus;

  @Inject
  ObjectMapper mapper;

  @POST
  @PermitAll
  @Consumes(MediaType.APPLICATION_JSON)
  public Response receiveEvent(KeycloakDTO dto, @HeaderParam("traceparent") String traceParent, @HeaderParam("tracestate") String traceState) {

    log.info("Webhook received: {}", (dto != null ? dto.getType() : "null"));

    if (dto == null || dto.getType() == null) {
      return Response.accepted().build();
    }

    List<String> eventsAllowed = List.of("REGISTER", "DELETE");

    if (!eventsAllowed.contains(dto.getType())) {
      return Response.accepted().build();
    }
    // 1. Convertir DTO externo a Payload interno
    WebhookKeycloakPayload payload = new WebhookKeycloakPayload(
        dto.getType(),
        dto.getUserId().toString(),
        dto.getClientId().toString(),
        mapper.convertValue(dto.getDetails(), new TypeReference<Map<String, String>>() {
        }),
        traceParent,
        traceState);

    // 2. Despachar al Bus (Fire and Forget)
    // "identity.webhook.keycloak" es la dirección del evento
    eventBus.send("identity.webhook.keycloak", payload);

    // 3. Responder rápido. Keycloak queda feliz.
    return Response.accepted().build();
  }
}
