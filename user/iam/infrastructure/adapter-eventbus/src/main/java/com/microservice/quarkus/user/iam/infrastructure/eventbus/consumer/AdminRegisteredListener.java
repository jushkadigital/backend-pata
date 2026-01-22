package com.microservice.quarkus.user.iam.infrastructure.eventbus.consumer;

import com.microservice.quarkus.user.iam.application.api.IdentityProvider;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class AdminRegisteredListener {

  @Inject
  IdentityProvider identityProvider;

  // Mapeo de AdminType a ruta de grupo
  private static final Map<String, String> ADMIN_TYPE_TO_GROUP = Map.of(
      "SUPER_ADMIN", "admin/super-admin",
      "ADMIN", "admin/admin",
      "EDITOR", "admin/editor");

  @ConsumeEvent("admin.registered")
  @Blocking
  public void onAdminRegistered(String event) {
    try {
      JsonObject jsonEvent = new JsonObject(event);
      System.out.println(jsonEvent.toString());
      String externalId = jsonEvent.getString("externalId");
      String email = jsonEvent.getString("email");
      String adminType = jsonEvent.getString("adminType");

      System.out.println("⚡ IAM Module: Recibido evento admin.registered para " + email + " con tipo " + adminType);

      // Obtener la ruta del grupo según el tipo de admin
      String groupPath = ADMIN_TYPE_TO_GROUP.get(adminType);

      if (groupPath == null) {
        System.err.println("❌ IAM Module: Tipo de admin desconocido: " + adminType);
        return;
      }

      // Obtener el ID del grupo
      // Nota: Necesitamos mantener un registro de los IDs de grupos o buscarlos
      // Por ahora asumimos que podemos buscar el grupo por su path
      String groupId = findGroupIdByPath(groupPath);

      if (groupId == null) {
        System.err.println("❌ IAM Module: No se encontró el grupo para la ruta: " + groupPath);
        return;
      }
      System.out.println("USSSER");
      System.out.println(externalId);
      // Asignar el usuario al grupo
      assignUserToGroup(externalId, groupId);

      System.out.println("✅ IAM Module: Usuario " + email + " asignado al grupo " + groupPath);

    } catch (Exception e) {
      System.err.println("❌ IAM Module: Error procesando evento admin.registered: " + e.getMessage());
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
