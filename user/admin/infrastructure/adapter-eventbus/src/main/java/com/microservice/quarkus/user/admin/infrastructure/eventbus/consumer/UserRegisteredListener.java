package com.microservice.quarkus.user.admin.infrastructure.eventbus.consumer;

import com.microservice.quarkus.user.admin.application.dto.CreateAdminCommand;
import com.microservice.quarkus.user.admin.application.service.AdminService;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserRegisteredListener {

  @Inject
  AdminService adminService;

  // Escucha el evento que viene del Scheduler/Outbox o del Bus directo
  @ConsumeEvent("iam.user.registered")
  @Blocking // Bloqueante porque vamos a escribir en BD
  public void onUserRegistered(String event) {

    JsonObject jsonEvent = new JsonObject(event);

    System.out.println(jsonEvent);

    // Transformamos el Evento (Infraestructura) a un Comando (Aplicación)
    if (jsonEvent.getString("type").equals("ADMIN")) {

      CreateAdminCommand cmd = (jsonEvent.mapTo(CreateAdminCommand.class));

      System.out.println(cmd);
      System.out.println("⚡ Admin Module: Recibido evento de usuario " + event);

      adminService.register(cmd);
      System.out.println("⚡ Admin Module: Admin Creado " + event);

    } else {
      System.out.println("NO DEBI ENTRAR");
    }

    // Transformamos el Evento (Infraestructura) a un Comando (Aplicación)

  }
}
