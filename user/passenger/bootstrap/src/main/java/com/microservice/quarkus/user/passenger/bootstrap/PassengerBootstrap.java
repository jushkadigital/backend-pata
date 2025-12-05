package com.microservice.quarkus.user.passenger.bootstrap;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@Startup
@ApplicationScoped
public class PassengerBootstrap {
  @PostConstruct
  void initModule() {
    System.out.println("INICIANDO PASSENGUER MODULEEE");
  }
}
