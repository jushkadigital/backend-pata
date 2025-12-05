package com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.control.MappingControl.Use;

import com.microservice.quarkus.user.passenger.domain.entities.Passenger;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerId;
import com.microservice.quarkus.user.passenger.domain.entities.EmailAddress;
import com.microservice.quarkus.user.passenger.infrastructure.db.hibernate.dbo.PassengerEntity;

@Mapper(componentModel = "cdi")
public interface PassengerMapper {

  default String mapToString(EmailAddress value) {
    return value.value(); // ⬅️ Extrae el String del Record
  }

  // 2. String -> VO (Método de conversión para DTO -> Domain)
  default EmailAddress mapToEmail(String emailString) {
    return new EmailAddress(emailString);
  }

  default PassengerId mapToUserId(String value) {
    return value != null ? new PassengerId(value) : null;
  }

  default String mapToString(PassengerId value) {
    return value != null ? value.value() : null;
  }

  @Mapping(source = "id", target = "id")
  @Mapping(source = "email", target = "email")
  PassengerEntity toDbo(Passenger domain);

  @Mapping(source = "id", target = "id")
  @Mapping(target = "email", source = "email")
  Passenger toDomain(PassengerEntity entity);

  void updateEntityFromDomain(Passenger domain, @MappingTarget PassengerEntity entity);

  @Mapping(target = "id", ignore = true)
  void updateDomainFromEntity(PassengerEntity entity, @MappingTarget Passenger domain);

}
