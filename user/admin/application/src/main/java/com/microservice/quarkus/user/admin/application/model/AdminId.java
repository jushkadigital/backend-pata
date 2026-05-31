package com.microservice.quarkus.user.admin.application.model;

import java.util.UUID;

public record AdminId(String value) {

  public AdminId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("El AdminId no puede ser nulo ni estar vacio.");
    }
  }

  public static AdminId random() {
    return new AdminId(UUID.randomUUID().toString());
  }

  public static AdminId of(String value) {
    return new AdminId(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
