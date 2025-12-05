package com.microservice.quarkus.user.admin.bootstrap;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@Startup
@ApplicationScoped
public class AdminBootstrap {
  @PostConstruct
  void initModule() {
    System.out.println("INICIANDO ADMIN MODULEEE");
  }
}
