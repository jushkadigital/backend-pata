package com.microservice.quarkus.user.identity.infrastructure.webhook.controler;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.quarkus.user.identity.application.api.ClientIdentityProvider;
import com.microservice.quarkus.user.identity.application.dto.CreateUserCommand;
import com.microservice.quarkus.user.identity.application.service.UserService;
import com.microservice.quarkus.user.identity.infrastructure.webhook.controler.dto.PayloadDTO;
import com.microservice.quarkus.user.identity.infrastructure.webhook.event.WebhookPayloadPayload;
import com.microservice.quarkus.user.shared.infrastructure.eventbus.CorrelationIdInterceptor;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.MDC;

@Path("/webhooks/payload")
public class PayloadCreateWebhookController {

  private static final Logger log = LoggerFactory.getLogger(PayloadCreateWebhookController.class);

  private final UserService userService;
  private final ClientIdentityProvider clientService;
  private final CorrelationIdInterceptor correlationIdInterceptor;

  @Inject
  ObjectMapper mapper;

  @ConfigProperty(name = "webhook.payload.secret")
  String webhookSecret;

  private static final Map<String, String> CLI_NAME_DICT = Map.of(
      "dashboard-client", "ADMIN",
      "frontend-client", "PASSENGER");

  @Inject
  public PayloadCreateWebhookController(UserService userService, ClientIdentityProvider clientService, CorrelationIdInterceptor correlationIdInterceptor) {
    this.userService = userService;
    this.clientService = clientService;
    this.correlationIdInterceptor = correlationIdInterceptor;
  }

  public record PayloadConsumerDTO(String providerid) {
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response receiveEvent(PayloadDTO dto, @Context HttpHeaders headers) {

    log.info("Webhook received: {}", (dto != null ? dto.getType() : "null"));

    String receivedSecret = headers.getHeaderString("X-Webhook-Secret");
    if (receivedSecret == null || !receivedSecret.equals(webhookSecret)) {
      log.info("Invalid webhook secret");
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("{\"error\": \"Invalid webhook secret\"}")
          .build();
    }

    if (dto == null || dto.getType() == null) {
      return Response.accepted().build();
    }

    WebhookPayloadPayload payload = new WebhookPayloadPayload(
        dto.getEmail(),
        dto.getPassword(),
        dto.getType(),
        dto.getClientId().toString());

    try {
      String correlationId = correlationIdInterceptor.getOrCreateCorrelationId();
      CreateUserCommand command = CreateUserCommand.fromWebhook2(payload.email(), payload.password(),
          CLI_NAME_DICT.get(clientService.getClientNameById(payload.clientId())), payload.type());
      String providerId = userService.register(command, correlationId);
      return Response.ok(new PayloadConsumerDTO(providerId)).build();

    } catch (Exception e) {
      throw e;
    }
  }
}
