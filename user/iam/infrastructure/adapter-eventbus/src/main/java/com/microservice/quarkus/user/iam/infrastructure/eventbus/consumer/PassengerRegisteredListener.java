package com.microservice.quarkus.user.iam.infrastructure.eventbus.consumer;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class PassengerRegisteredListener {

  @Inject
  IdentityProvider identityProvider;

  // Mapeo de PassengerType a ruta de grupo
  private static final Map<String, String> PASSENGER_TYPE_TO_GROUP = Map.of(
      "PREMIUM", "passenger/premium",
      "STANDARD", "passenger/standard",
      "BASIC", "passenger/basic");

  @ConsumeEvent("passenger.registered")
  @Blocking
  public void onPassengerRegistered(String event) {
    try {
      JsonObject jsonEvent = new JsonObject(event);

      String externalId = jsonEvent.getString("externalId");
      String email = jsonEvent.getString("email");
      String passengerType = jsonEvent.getString("passengerType");

      System.out.println("⚡ IAM Module: Recibido evento passenger.registered para " + email + " con tipo " + passengerType);

      // Obtener la ruta del grupo según el tipo de passenger
      String groupPath = PASSENGER_TYPE_TO_GROUP.get(passengerType);

      if (groupPath == null) {
        System.err.println("❌ IAM Module: Tipo de passenger desconocido: " + passengerType);
        return;
      }

      // Obtener el ID del grupo
      String groupId = findGroupIdByPath(groupPath);

      if (groupId == null) {
        System.err.println("❌ IAM Module: No se encontró el grupo para la ruta: " + groupPath);
        return;
      }

      // Asignar el usuario al grupo
      assignUserToGroup(externalId, groupId);

      System.out.println("✅ IAM Module: Usuario " + email + " asignado al grupo " + groupPath);

    } catch (Exception e) {
      System.err.println("❌ IAM Module: Error procesando evento passenger.registered: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Busca el ID de un grupo por su ruta
   */
  private String findGroupIdByPath(String groupPath) {
    return identityProvider.findGroupByPath(groupPath);
  }

  /**
   * Asigna un usuario a un grupo en Keycloak
   */
  private void assignUserToGroup(String userId, String groupId) {
    identityProvider.assignUserToGroup(userId, groupId);
  }
}
