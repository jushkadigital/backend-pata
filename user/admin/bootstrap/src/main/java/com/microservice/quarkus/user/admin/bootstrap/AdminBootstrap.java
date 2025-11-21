package com.microservice.quarkus.user.admin.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AdminBootstrap {
  public void initModule() {
    System.out.println("GAAA");
  }
}
