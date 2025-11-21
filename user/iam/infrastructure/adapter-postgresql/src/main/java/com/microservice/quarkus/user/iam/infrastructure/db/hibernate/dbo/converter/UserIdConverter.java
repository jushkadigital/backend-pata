package com.microservice.quarkus.user.iam.infrastructure.db.hibernate.dbo.converter;

import com.microservice.quarkus.user.iam.domain.UserId;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UserIdConverter implements AttributeConverter<UserId, String> {

  @Override
  public String convertToDatabaseColumn(UserId attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.value(); // Extrae el String del Record
  }

  @Override
  public UserId convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    // El constructor del Record UserId valida si es un UUID válido
    return new UserId(dbData);
  }
}
