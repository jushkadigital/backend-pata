package com.microservice.quarkus.user.admin.bootstrap;

import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class AdminBootstrap {
  void onStart(@Observes @Priority(30) StartupEvent ev) {
    System.out.println("INICIANDO ADmin MODULEEE");
    // Tu lógica de inicialización aquí
  }

}
