package com.microservice.quarkus.user.iam.domain;

import java.util.regex.Pattern;

// Record es ideal para VOs: inmutable, equals(), hashCode() y constructor conciso.
public record EmailAddress(String value) {

  private static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
      Pattern.CASE_INSENSITIVE);

  // Constructor compacto (se ejecuta automáticamente para validación)
  public EmailAddress {
    if (value == null || !VALID_EMAIL_ADDRESS_REGEX.matcher(value).matches()) {
      throw new IllegalArgumentException("El formato del email no es válido.");
    }
  }
}
