package com.microservice.quarkus.user.iam.domain;

// Usa un Record simple, ya que el ID no necesita comportamiento complejo.
public record UserId(String value) {

  // Opcional: Puedes añadir validación de formato (ej: asegurar que sea un UUID)
  public UserId {
    if (value == null || value.length() != 36) { // Validación simple para UUID
      throw new IllegalArgumentException("El ID de usuario debe ser un UUID válido.");
    }
  }
}
