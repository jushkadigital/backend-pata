package com.microservice.quarkus.catalogo.application.service;

import com.microservice.quarkus.catalogo.domain.shared.ValueObjectFactory;
import org.jboss.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class PluginManager {

  private final Logger log;
  @Inject
  Instance<ValueObjectFactory<?>> allFactories; // CDI descubre todos.
  private Map<String, ValueObjectFactory<?>> factoryMap; // Cache por nombre/clase.

  public PluginManager(Instance<ValueObjectFactory<?>> allFactories, Logger log) {
    this.allFactories = allFactories;
    this.log = log;
  }

  @PostConstruct
  public void init() {
    System.out.println("UUUUUU");
    log.info("GAAAA");
    factoryMap = allFactories.stream()
        .collect(Collectors.toMap(f -> f.getClass().getSimpleName(), f -> f));
    log.info(factoryMap);
  }

  public Map<String, ValueObjectFactory<?>> getFactoryMap() {
    System.out.println("MAAAAAA");
    log.info(factoryMap);
    return factoryMap;
  }
}
