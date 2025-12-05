package com.microservice.quarkus.user.passenger.infrastructure.eventbus.consumer;

import com.microservice.quarkus.user.passenger.application.dto.CreatePassengerCommand;
import com.microservice.quarkus.user.passenger.application.service.PassengerService;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserRegisteredListener {

  @Inject
  PassengerService adminService;

  // Escucha el evento que viene del Scheduler/Outbox o del Bus directo
  @ConsumeEvent("iam.user.registered")
  @Blocking // Bloqueante porque vamos a escribir en BD
  public void onUserRegistered(String event) {

    JsonObject jsonEvent = new JsonObject(event);

    System.out.println(jsonEvent);

    CreatePassengerCommand cmd = (jsonEvent.mapTo(CreatePassengerCommand.class));

    System.out.println(cmd);
    System.out.println("⚡ passenger Module: Recibido evento de usuario " + event);

    // Transformamos el Evento (Infraestructura) a un Comando (Aplicación)

  }
}
