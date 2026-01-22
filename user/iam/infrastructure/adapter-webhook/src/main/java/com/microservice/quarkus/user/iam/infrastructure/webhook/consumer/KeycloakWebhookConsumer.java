package com.microservice.quarkus.user.iam.infrastructure.webhook.consumer;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import com.microservice.quarkus.user.iam.application.dto.CreateUserCommand;
import com.microservice.quarkus.user.iam.application.service.UserService;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserType;
import com.microservice.quarkus.user.iam.infrastructure.webhook.event.WebhookKeycloakPayload;
import com.microservice.quarkus.user.iam.infrastructure.webhook.event.WebhookPayloadPayload;

@ApplicationScoped
public class KeycloakWebhookConsumer {

  private static final Logger LOG = Logger.getLogger(KeycloakWebhookConsumer.class);

  private final Map<String, String> cliNameDict = new HashMap<>() {
    {
      put("dashboard-client", "ADMIN");
      put("frontend-client", "PASSENGER");
    }
  };

  @Inject
  IdentityProvider clientService;

  @Inject
  UserService userService;

  /**
   * Escucha la dirección "iam.webhook.keycloak".
   * 
   * @Blocking es vital si tu UseCase accede a Base de Datos (JPA es bloqueante).
   */
  @ConsumeEvent("iam.webhook.keycloak")
  @Blocking
  public void processKeycloakEvent(WebhookKeycloakPayload payload) {

    LOG.infof("Procesando evento asíncrono: %s para usuario %s", payload.eventType(), payload.userId());

    User tempUser = clientService.getUserById(payload.userId());
    try {
      // Lógica de filtrado
      if ("REGISTER".equals(payload.eventType())) {

        CreateUserCommand command = CreateUserCommand.fromWebhook(
            tempUser.getEmail().value(),
            tempUser.getExternalId(),
            cliNameDict.get(clientService.getClientNameById(payload.clientId())));

        // Invocar Dominio
        userService.register(command);

        LOG.info("Sincronización exitosa.");
      }
    } catch (Exception e) {
      // Si lanzas excepción aquí, @Retry se activa.
      LOG.error("Fallo al procesar webhook. Reintentando...", e);
      throw e;
    }
  }

  @ConsumeEvent("iam.webhook.payload")
  @Blocking
  public void processPayloadEvent(WebhookPayloadPayload payload) {

    LOG.infof("Procesando evento asíncrono:  para usuario %s", payload.email());

    try {
      // Lógica de filtrado
      CreateUserCommand command = CreateUserCommand.fromWebhook2(payload.email(), payload.password(),
          cliNameDict.get(clientService.getClientNameById(payload.clientId())), payload.type());

      userService.register(command);

      LOG.info("Sincronización exitosa.");
    } catch (Exception e) {
      // Si lanzas excepción aquí, @Retry se activa.
      LOG.error("Fallo al procesar webhook. Reintentando...", e);
      throw e;
    }
  }
}
