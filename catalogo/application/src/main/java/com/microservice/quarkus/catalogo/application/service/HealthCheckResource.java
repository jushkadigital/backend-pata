package com.microservice.quarkus.catalogo.application.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Path("/health")
public class HealthCheckResource {

  // ➡️ Al inyectar PluginManager aquí, fuerzas su creación.
  @Inject
  PluginManager pluginManager;

  public HealthCheckResource(PluginManager pluginManager) {
    this.pluginManager = pluginManager;
  }

  @GET
  public String check() {
    System.out.println("AGUUUUU");
    // La inyección ya ocurrió en la fase de inicio.
    // Ahora puedes usar el bean si es necesario.

    pluginManager.getFactoryMap(); // Llama a un método para usar el bean
    return "Application is running and PluginManager has been created!";
  }
}
