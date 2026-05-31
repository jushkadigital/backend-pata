package com.microservices.quarkus.config.bootloader;

import com.microservice.quarkus.user.identity.bootstrap.IdentityBootstrap;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AppContext {

  @PostConstruct
  void init() {
    System.out.println("MONOOO");
  }

}
