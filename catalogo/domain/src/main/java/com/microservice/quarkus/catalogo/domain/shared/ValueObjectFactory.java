package com.microservice.quarkus.catalogo.domain.shared;

public interface ValueObjectFactory<T> {
  T create(Object... args);
}
