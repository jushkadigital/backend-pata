
package com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo.converter;

import com.microservice.quarkus.admin.domain.entities.AdminId;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AdminIdConverter implements AttributeConverter<AdminId, String> {

  @Override
  public String convertToDatabaseColumn(AdminId attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.value(); // Extrae el String del Record
  }

  @Override
  public AdminId convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    // El constructor del Record UserId valida si es un UUID válido
    return new AdminId(dbData);
  }
}
