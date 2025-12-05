package com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.dbo.converter;

import com.microservice.quarkus.user.passenger.domain.entities.EmailAddress;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EmailAddressConverter implements AttributeConverter<EmailAddress, String> {

  @Override
  public String convertToDatabaseColumn(EmailAddress attribute) {
    // Asegura que este código sea correcto
    return attribute.value();
  }

  @Override
  public EmailAddress convertToEntityAttribute(String dbData) {
    return new EmailAddress(dbData);
  }
}
