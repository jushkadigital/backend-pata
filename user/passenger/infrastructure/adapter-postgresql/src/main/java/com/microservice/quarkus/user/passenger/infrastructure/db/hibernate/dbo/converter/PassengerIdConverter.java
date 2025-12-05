
package com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.dbo.converter;

import com.microservice.quarkus.user.passenger.domain.entities.PassengerId;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PassengerIdConverter implements AttributeConverter<PassengerId, String> {

  @Override
  public String convertToDatabaseColumn(PassengerId attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.value(); // Extrae el String del Record
  }

  @Override
  public PassengerId convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    // El constructor del Record UserId valida si es un UUID válido
    return new PassengerId(dbData);
  }
}
