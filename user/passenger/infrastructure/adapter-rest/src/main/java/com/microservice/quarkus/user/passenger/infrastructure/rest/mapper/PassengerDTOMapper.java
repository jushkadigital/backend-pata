package com.microservice.quarkus.user.passenger.infrastructure.rest.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.microservice.quarkus.user.passenger.application.dto.CompletePassengerCommand;
import com.microservice.quarkus.user.passenger.domain.entities.EmailAddress;
import com.microservice.quarkus.user.passenger.domain.entities.Passenger;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerId;
import com.microservice.quarkus.user.passenger.domain.entities.PassengerStatus;
import com.microservice.quarkus.user.passenger.infrastructure.rest.dto.CompleteRequestDTO;
import com.microservice.quarkus.user.passenger.infrastructure.rest.dto.MeDTO;

@Mapper(componentModel = "cdi") // Importante para inyección en Quarkus
public interface PassengerDTOMapper {

  // Mapea el UserRepresentation (Keycloak) al objeto User (Dominio)
  default String mapToString(EmailAddress value) {
    return value.value(); // ⬅️ Extrae el String del Record
  }

  default UUID mapToString(PassengerId value) {
    return value != null ? UUID.fromString(value.value()) : null;
  }

  @Mapping(target = "id", source = "id")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "onboardingCompleted", expression = "java(checkCompletion(passenger))")
  MeDTO toDto(Passenger passenger);

  default boolean checkCompletion(Passenger passenger) {
    return passenger.getStatus() == PassengerStatus.ACTIVE;
  }

  @Mapping(target = "firstNames", source = "firstNames")
  @Mapping(target = "lastNames", source = "lastNames")
  @Mapping(target = "dni", source = "dni")
  @Mapping(target = "phone", source = "phone")
  CompletePassengerCommand toDomain(CompleteRequestDTO dto);
}
