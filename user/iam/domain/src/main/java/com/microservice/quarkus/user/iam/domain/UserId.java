package com.microservice.quarkus.user.iam.domain;

import java.util.UUID;

// Usa un Record simple, ya que el ID no necesita comportamiento complejo.
public record UserId(String value) {

  // Opcional: Puedes añadir validación de formato (ej: asegurar que sea un UUID)
  public UserId {
    if (value == null || value.isBlank()) { // Validación simple para UUID
      throw new IllegalArgumentException("El ID de usuario debe ser un UUID válido.");
    }
  }

  // 2. Constructor Compacto: Garantiza que NUNCA existirá un UserId inválido

  // 3. Factory Method para crear NUEVOS identificadores (Semántica clara)
  public static UserId random() {
    return new UserId(UUID.randomUUID().toString());
  }

  // 4. Factory Method para reconstruir IDs existentes (ej. desde base de datos o
  // JSON)
  public static UserId of(String value) {
    return new UserId(value);
  }

  // 5. toString devuelve el valor puro (útil para logs y depuración simple)
  @Override
  public String toString() {
    return value;
  }
}
