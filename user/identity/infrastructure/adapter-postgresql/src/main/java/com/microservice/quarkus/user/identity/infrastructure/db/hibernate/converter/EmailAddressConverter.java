package com.microservice.quarkus.user.identity.infrastructure.db.hibernate.converter;

import com.microservice.quarkus.user.shared.domain.EmailAddress;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EmailAddressConverter implements AttributeConverter<EmailAddress, String> {

  @Override
  public String convertToDatabaseColumn(EmailAddress attribute) {
    return attribute.value();
  }

  @Override
  public EmailAddress convertToEntityAttribute(String dbData) {
    return new EmailAddress(dbData);
  }
}
