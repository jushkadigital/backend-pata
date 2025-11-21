package com.microservice.quarkus.admin.domain.entities;

import java.util.UUID;

// 1. Definición concisa usando 'record'
public record AdminId(String value) {

  // 2. Constructor Compacto: Garantiza que NUNCA existirá un AdminId inválido
  public AdminId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("El AdminId no puede ser nulo ni estar vacío.");
    }
  }

  // 3. Factory Method para crear NUEVOS identificadores (Semántica clara)
  public static AdminId random() {
    return new AdminId(UUID.randomUUID().toString());
  }

  // 4. Factory Method para reconstruir IDs existentes (ej. desde base de datos o
  // JSON)
  public static AdminId of(String value) {
    return new AdminId(value);
  }

  // 5. toString devuelve el valor puro (útil para logs y depuración simple)
  @Override
  public String toString() {
    return value;
  }
}
