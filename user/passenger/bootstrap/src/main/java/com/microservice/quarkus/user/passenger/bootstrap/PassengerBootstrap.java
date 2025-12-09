package com.microservice.quarkus.user.passenger.bootstrap;

import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class PassengerBootstrap {
  void onStart(@Observes @Priority(20) StartupEvent ev) {
    System.out.println("INICIANDO PASSENGUER MODULEEE");
    // Tu lógica de inicialización aquí
  }
}
